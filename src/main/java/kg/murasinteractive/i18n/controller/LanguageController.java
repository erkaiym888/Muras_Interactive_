package kg.murasinteractive.i18n.controller;

import kg.murasinteractive.i18n.dto.I18nDtos.LanguageDto;
import kg.murasinteractive.i18n.dto.I18nDtos.LanguagesDto;
import kg.murasinteractive.i18n.service.LanguageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/languages - включённые языки для переключателя (FR-8.1).
 * Пока включён только русский, переключатель на сайте не показывается.
 */
@RestController
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping("/api/languages")
    public LanguagesDto languages() {
        return new LanguagesDto(languageService.defaultCode(), languageService.enabled().stream()
                .map(l -> new LanguageDto(l.getCode(), l.getNativeName())).toList());
    }
}
