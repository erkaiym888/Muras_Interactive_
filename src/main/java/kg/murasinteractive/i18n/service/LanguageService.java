package kg.murasinteractive.i18n.service;

import jakarta.servlet.http.HttpServletRequest;
import kg.murasinteractive.i18n.model.Language;
import kg.murasinteractive.i18n.repository.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Locale;

/**
 * Какой язык нужен текущему запросу (FR-8.1).
 * Порядок: параметр ?lang=ky, заголовок X-Lang (его ставит фронтенд), заголовок Accept-Language браузера.
 * Подходит только включённый язык; иначе - язык по умолчанию (русский).
 */
@Service
public class LanguageService {

    public static final String FALLBACK = "ru";
    private static final String REQUEST_ATTR = LanguageService.class.getName() + ".current";

    /** Язык по умолчанию не меняется во время работы - запоминаем после первого чтения. */
    private volatile String defaultCode;

    private final LanguageRepository languageRepository;

    public LanguageService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Transactional(readOnly = true)
    public List<Language> enabled() {
        return languageRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public String defaultCode() {
        String code = defaultCode;
        if (code == null) {
            code = languageRepository.findAllByOrderBySortOrderAsc().stream()
                    .filter(Language::isDefaultLanguage).map(Language::getCode).findFirst().orElse(FALLBACK);
            defaultCode = code;
        }
        return code;
    }

    /** Язык текущего HTTP-запроса; вне запроса (планировщик, сидеры) - язык по умолчанию. */
    @Transactional(readOnly = true)
    public String current() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servlet)) return defaultCode();
        HttpServletRequest request = servlet.getRequest();
        // В одном запросе язык спрашивают много раз - определяем один раз
        if (request.getAttribute(REQUEST_ATTR) instanceof String cached) return cached;
        String code = resolve(request);
        request.setAttribute(REQUEST_ATTR, code);
        return code;
    }

    private String resolve(HttpServletRequest request) {
        List<String> codes = enabled().stream().map(Language::getCode).toList();
        for (String candidate : new String[]{request.getParameter("lang"), request.getHeader("X-Lang")}) {
            String code = normalize(candidate);
            if (code != null && codes.contains(code)) return code;
        }
        String accept = request.getHeader("Accept-Language");
        if (accept != null) {
            for (String part : accept.split(",")) {
                String code = normalize(part.split(";")[0]);
                if (code != null && codes.contains(code)) return code;
            }
        }
        return defaultCode();
    }

    /** "ky-KG" -> "ky", " EN " -> "en". */
    static String normalize(String tag) {
        if (tag == null || tag.isBlank()) return null;
        String code = tag.trim().toLowerCase(Locale.ROOT);
        int dash = code.indexOf('-');
        return dash > 0 ? code.substring(0, dash) : code;
    }
}
