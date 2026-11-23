package kg.murasinteractive.search.service;

import kg.murasinteractive.search.dto.SearchResponseDto;
import kg.murasinteractive.search.dto.SearchResultDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Показываются только опубликованные обряды. Фронтенд показывает результаты выпадающим списком под строкой
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

    /** Общая часть запросов: слова запроса (q) и все материалы с их текстом и весами (docs). */
    private static final String DOCS = """
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
              union all
              select 'work', w.work_id, null, w.title, t.type_name, coalesce(w.summary, ''),
                     setweight(to_tsvector('russian', coalesce(w.title, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(w.title, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(w.summary, '')), 'B')
                from folklore_work w join folklore_type t on t.folklore_type_id = w.work_type_id
              union all
              select 'yurt', null, y.element_key, y.name, y.element_group, coalesce(y.description, ''),
                     setweight(to_tsvector('russian', coalesce(y.name, '')), 'A')
                       || setweight(to_tsvector('simple', coalesce(y.name, '')), 'A')
                       || setweight(to_tsvector('russian', coalesce(y.description, '')), 'B')
                from yurt_element y
            )
            """.formatted(STEPS);

    /** Условие совпадения + фильтр по видам материалов (FR-4.3). */
    private static final String MATCH = """
             where (d.doc @@ q.tsq or d.title ilike :like escape '\\')
               and (:allKinds or d.kind in (:kinds))
            """;

    private static final String SQL = DOCS + """
            select d.kind, d.id, d.key, d.title, d.subtitle,
                   ts_headline('russian', d.body, q.tsq,
                     'StartSel=[[, StopSel=]], MaxWords=26, MinWords=12, ShortWord=2, MaxFragments=1, FragmentDelimiter=" … "') as snippet
              from docs d cross join q
            """ + MATCH + """
             order by ts_rank_cd(d.doc, q.tsq) + case when d.title ilike :like escape '\\' then 1 else 0 end desc, d.title
             limit :limit
            """;

    /** Сколько найдено каждого вида - для групп и фильтра в выпадающем списке (FR-4.3). Без фильтра по виду. */
    private static final String COUNTS = DOCS + """
            select d.kind, count(*) as n
              from docs d cross join q
             where d.doc @@ q.tsq or d.title ilike :like escape '\\'
             group by d.kind
            """;

    /** Виды материалов в порядке групп. */
    public static final List<String> KINDS = List.of("rite", "hero", "work", "yurt");

    /** Все слова текстов сайта (из опубликованного) - словарь для исправления опечаток. */
    private static final String SPLIT = "regexp_split_to_table(lower(concat_ws(' ', %s)), '[^[:alpha:][:digit:]]+')";
    private static final String VOCAB = """
            with vocab as (
              select distinct w from (
                select %1$s as w from rite r where r.status = 'PUBLISHED'
                union all select %2$s from rite_step s join rite r on r.rite_id = s.rite_id where r.status = 'PUBLISHED'
                union all select %3$s from folklore_hero h
                union all select %4$s from folklore_work fw
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
        return search(query, limit, null);
    }

    /**
     * @param kinds какие виды материалов показать (rite, hero, work, yurt); пусто - все (FR-4.3).
     *              Счётчики по видам считаются всегда по всем видам - для групп и фильтра.
     */
    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, Integer limit, List<String> kinds) {
        int max = limit == null ? LIMIT : Math.max(1, Math.min(limit, LIMIT));
        String clean = query == null ? "" : query.trim();
        List<String> words = words(clean);
        List<String> only = kinds == null ? List.of() : kinds.stream().filter(KINDS::contains).distinct().toList();
        if (clean.length() < MIN_LENGTH || words.isEmpty()) {
            return new SearchResponseDto(clean, null, 0, emptyCounts(), List.of());
        }
        List<String> used = words;
        String phrase = clean;
        List<SearchResultDto> results = new ArrayList<>(find(words, clean, max, only));
        String corrected = null;
        // Опечатки (FR-4.2): мало найдено - пробуем исправленный запрос и добавляем найденное после точного
        if (results.size() < max && fuzzy.available()) {
            List<String> fixed = correct(words);
            if (!fixed.equals(words)) {
                Set<String> seen = new HashSet<>();
                results.forEach(r -> seen.add(key(r)));
                int before = results.size();
                for (SearchResultDto r : find(fixed, String.join(" ", fixed), max, only)) {
                    if (results.size() >= max) break;
                    if (seen.add(key(r))) results.add(r);
                }
                if (results.size() > before) {
                    corrected = String.join(" ", fixed);
                    if (before == 0) { used = fixed; phrase = corrected; }
                }
            }
        }
        Map<String, Integer> counts = counts(used, phrase);
        int total = only.isEmpty() ? counts.values().stream().mapToInt(Integer::intValue).sum()
                : only.stream().mapToInt(k -> counts.getOrDefault(k, 0)).sum();
        // Группы (FR-4.3): результаты по видам в постоянном порядке, внутри вида - по релевантности
        List<SearchResultDto> grouped = results.stream()
                .sorted(Comparator.comparingInt((SearchResultDto r) -> KINDS.indexOf(r.kind())))
                .toList();
        return new SearchResponseDto(clean, corrected, Math.max(total, grouped.size()), counts, grouped);
    }

    private List<SearchResultDto> find(List<String> words, String phrase, int limit, List<String> kinds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tsq", toTsQuery(words))
                .addValue("like", "%" + escapeLike(phrase) + "%")
                .addValue("allKinds", kinds.isEmpty())
                .addValue("kinds", kinds.isEmpty() ? KINDS : kinds)
                .addValue("limit", limit);
        return jdbc.query(SQL, params, (rs, i) -> new SearchResultDto(
                rs.getString("kind"),
                rs.getObject("id") == null ? null : rs.getLong("id"),
                rs.getString("key"),
                rs.getString("title"),
                rs.getString("subtitle"),
                rs.getString("snippet")));
    }

    /** Сколько найдено каждого вида (все виды, даже нулевые - в постоянном порядке). */
    private Map<String, Integer> counts(List<String> words, String phrase) {
        Map<String, Integer> counts = emptyCounts();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tsq", toTsQuery(words))
                .addValue("like", "%" + escapeLike(phrase) + "%");
        jdbc.query(COUNTS, params, rs -> { counts.put(rs.getString("kind"), rs.getInt("n")); });
        return counts;
    }

    private static Map<String, Integer> emptyCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        KINDS.forEach(k -> counts.put(k, 0));
        return counts;
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
