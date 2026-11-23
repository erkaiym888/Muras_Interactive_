package kg.murasinteractive.search.service;

import kg.murasinteractive.search.dto.SearchResponseDto;
import kg.murasinteractive.search.dto.SearchResultDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Полнотекстовый поиск по всем разделам (FR-4.1) средствами PostgreSQL.
 * Ищем по названиям и описаниям обрядов (вместе с этапами), героев, произведений и элементов юрты.
 * - словоформы: словарь russian со стеммингом («невеста» найдёт «невесты», «невесту»);
 * - начало слова: каждое слово запроса ищется как префикс («колыб» найдёт «колыбель»);
 * - кыргызские названия (түндүк, бешик) дополнительно ищутся словарём simple без стемминга;
 * - на случай редких слов - совпадение подстроки в названии.
 * Название весит больше описания (веса A/B/C), совпадение в названии поднимает результат наверх.
 * Показываются только опубликованные обряды, герои и произведения (статусы FR-7.2). Фронтенд показывает результаты выпадающим списком под строкой
 * поиска в шапке (как в Gmail).
 *
 * Опечатки (FR-4.2): если по запросу как есть найдено мало, каждое незнакомое слово заменяется
 * ближайшим словом из текстов сайта по расстоянию Левенштейна (fuzzystrmatch): до 1 ошибки в словах
 * из 4-5 букв, до 2 - в более длинных; слово при наборе сравнивается и с началом слов («манс» -> «манас»).
 * Результаты по исправленному запросу добавляются после точных, исправление возвращается в corrected.
 */
@Service
public class SearchService {

    public static final int MIN_LENGTH = 2;
    public static final int LIMIT = 50;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    /** Тексты этапов обряда одной строкой. */
    private static final String STEPS = "(select string_agg(concat_ws(' ', s.step_title, s.step_text), ' ') from rite_step s where s.rite_id = r.rite_id)";

    private static final String SQL = """
            with q as (select (to_tsquery('russian', :tsq) || to_tsquery('simple', :tsq)) as tsq),
            docs as (
              select 'rite'::text as kind, r.rite_id::bigint as id, null::text as key, r.title::text as title,
                     c.category_name::text as subtitle,
                     concat_ws(' ', r.summary, r.description, r.symbolism, %1$s) as body,
                     setweight(to_tsvector('russian', coalesce(r.title, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(r.title, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(r.summary, '')), 'B')
                       || setweight(to_tsvector('russian', concat_ws(' ', r.description, r.symbolism, %1$s)), 'C') as doc
                from rite r join rite_category c on c.rite_category_id = r.rite_category_id
               where r.status = 'PUBLISHED'
              union all
              select 'hero', h.hero_id, null, h.name, t.type_name, coalesce(h.description, ''),
                     setweight(to_tsvector('russian', coalesce(h.name, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(h.name, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(h.description, '')), 'B')
                from folklore_hero h join folklore_type t on t.folklore_type_id = h.hero_type_id
               where h.status = 'PUBLISHED'
              union all
              select 'work', w.work_id, null, w.title, t.type_name, coalesce(w.summary, ''),
                     setweight(to_tsvector('russian', coalesce(w.title, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(w.title, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(w.summary, '')), 'B')
                from folklore_work w join folklore_type t on t.folklore_type_id = w.work_type_id
               where w.status = 'PUBLISHED'
              union all
              select 'yurt', null, y.element_key, y.name, y.element_group, coalesce(y.description, ''),
                     setweight(to_tsvector('russian', coalesce(y.name, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(y.name, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(y.description, '')), 'B')
                from yurt_element y
            )
            select d.kind, d.id, d.key, d.title, d.subtitle,
                   ts_headline('russian', d.body, q.tsq,
                     'StartSel=[[, StopSel=]], MaxWords=26, MinWords=12, ShortWord=2, MaxFragments=1, FragmentDelimiter=" … "') as snippet
              from docs d cross join q
             where d.doc @@ q.tsq or d.title ilike :like escape '\\'
             order by ts_rank_cd(d.doc, q.tsq) + case when d.title ilike :like escape '\\' then 1 else 0 end desc, d.title
             limit :limit
            """.formatted(STEPS);

    /** Все слова текстов сайта (из опубликованного) - словарь для исправления опечаток. */
    private static final String SPLIT = "regexp_split_to_table(lower(concat_ws(' ', %s)), '[^[:alpha:][:digit:]]+')";
    private static final String VOCAB = """
            with vocab as (
              select distinct w from (
                select %1$s as w from rite r where r.status = 'PUBLISHED'
                union all select %2$s from rite_step s join rite r on r.rite_id = s.rite_id where r.status = 'PUBLISHED'
                union all select %3$s from folklore_hero h where h.status = 'PUBLISHED'
                union all select %4$s from folklore_work fw where fw.status = 'PUBLISHED'
                union all select %5$s from yurt_element y
              ) t where length(w) >= 3
            )
            """.formatted(
            SPLIT.formatted("r.title, r.summary, r.description, r.symbolism"),
            SPLIT.formatted("s.step_title, s.step_text"),
            SPLIT.formatted("h.name, h.description"),
            SPLIT.formatted("fw.title, fw.summary"),
            SPLIT.formatted("y.name, y.description"));

    /** Есть ли в текстах слово, начинающееся так же, - тогда слово считаем написанным верно. */
    private static final String KNOWN = VOCAB + "select exists(select 1 from vocab where w like :prefix escape '\\')";

    /** Ближайшее слово: сначала по числу ошибок, потом по близости длины. Сравниваем и с началом слова (ввод не закончен). */
    private static final String NEAREST = VOCAB + """
            select w from vocab
             where least(levenshtein(w, :word), levenshtein(left(w, length(:word)), :word)) <= :maxd
             order by least(levenshtein(w, :word), levenshtein(left(w, length(:word)), :word)),
                      abs(length(w) - length(:word)), w
             limit 1
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final FuzzySetup fuzzy;

    public SearchService(NamedParameterJdbcTemplate jdbc, FuzzySetup fuzzy) {
        this.jdbc = jdbc;
        this.fuzzy = fuzzy;
    }

    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, Integer limit) {
        int max = limit == null ? LIMIT : Math.max(1, Math.min(limit, LIMIT));
        String clean = query == null ? "" : query.trim();
        List<String> words = words(clean);
        if (clean.length() < MIN_LENGTH || words.isEmpty()) {
            return new SearchResponseDto(clean, null, 0, List.of());
        }
        // на один больше - чтобы понять, есть ли ещё
        List<SearchResultDto> results = new ArrayList<>(find(words, clean, max + 1));
        String corrected = null;
        if (results.size() <= max && fuzzy.available()) {
            List<String> fixed = correct(words);
            if (!fixed.equals(words)) {
                Set<String> seen = new HashSet<>();
                results.forEach(r -> seen.add(key(r)));
                int before = results.size();
                for (SearchResultDto r : find(fixed, String.join(" ", fixed), max + 1)) {
                    if (results.size() > max) break;
                    if (seen.add(key(r))) results.add(r);
                }
                if (results.size() > before) corrected = String.join(" ", fixed);
            }
        }
        boolean more = results.size() > max;
        List<SearchResultDto> shown = more ? results.subList(0, max) : results;
        // total: сколько показано, или «больше», если есть ещё (точный подсчёт для списка не нужен)
        return new SearchResponseDto(clean, corrected, more ? max + 1 : shown.size(), List.copyOf(shown));
    }

    private List<SearchResultDto> find(List<String> words, String phrase, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tsq", toTsQuery(words))
                .addValue("like", "%" + escapeLike(phrase) + "%")
                .addValue("limit", limit);
        return jdbc.query(SQL, params, (rs, i) -> new SearchResultDto(
                rs.getString("kind"),
                rs.getObject("id") == null ? null : rs.getLong("id"),
                rs.getString("key"),
                rs.getString("title"),
                rs.getString("subtitle"),
                rs.getString("snippet")));
    }

    /** Исправляет слова, которых нет в текстах сайта; знакомые и короткие оставляет как есть. */
    private List<String> correct(List<String> words) {
        List<String> fixed = new ArrayList<>();
        for (String w : words) {
            int maxd = maxDistance(w);
            if (maxd == 0 || isKnown(w)) { fixed.add(w); continue; }
            List<String> near = jdbc.queryForList(NEAREST, new MapSqlParameterSource()
                    .addValue("word", w).addValue("maxd", maxd), String.class);
            fixed.add(near.isEmpty() ? w : near.get(0));
        }
        return fixed;
    }

    private boolean isKnown(String word) {
        Boolean known = jdbc.queryForObject(KNOWN, new MapSqlParameterSource("prefix", escapeLike(word) + "%"), Boolean.class);
        return Boolean.TRUE.equals(known);
    }

    /** Сколько ошибок допускаем: в коротких словах опечатку не угадать, в длинных - до 2 (п. FR-4.2 ТЗ). */
    static int maxDistance(String word) {
        int n = word.length();
        return n < 4 ? 0 : n <= 5 ? 1 : 2;
    }

    private static String key(SearchResultDto r) {
        return r.kind() + ":" + (r.key() != null ? r.key() : r.id());
    }

    /** Слова запроса без знаков препинания, в нижнем регистре. */
    static List<String> words(String query) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(query.toLowerCase(Locale.ROOT));
        while (m.find() && words.size() < 8) words.add(m.group());
        return words;
    }

    /** "невеста проводы" -> "невеста:* & проводы:*": все слова, каждое - как начало слова. */
    static String toTsQuery(List<String> words) {
        return String.join(" & ", words.stream().map(w -> w + ":*").toList());
    }

    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
