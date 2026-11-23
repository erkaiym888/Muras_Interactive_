package kg.murasinteractive.i18n.controller;

import kg.murasinteractive.i18n.dto.I18nDtos.AdminLanguageDto;
import kg.murasinteractive.i18n.dto.I18nDtos.LanguageToggleRequest;
import kg.murasinteractive.i18n.model.Language;
import kg.murasinteractive.i18n.repository.ContentTranslationRepository;
import kg.murasinteractive.i18n.repository.LanguageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Языки сайта в панели администратора (FR-8.1), только ADMIN.
 *  GET   /api/admin/languages         - все языки и число переводов
 *  PATCH /api/admin/languages/{code}  - включить или выключить язык ({"enabled": true})
 */
@RestController
@RequestMapping("/api/admin/languages")
public class AdminLanguageController {

    private final LanguageRepository languageRepository;
    private final ContentTranslationRepository translationRepository;

    public AdminLanguageController(LanguageRepository languageRepository, ContentTranslationRepository translationRepository) {
        this.languageRepository = languageRepository;
        this.translationRepository = translationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminLanguageDto> languages() {
        return languageRepository.findAllByOrderBySortOrderAsc().stream().map(this::toDto).toList();
    }

    @PatchMapping("/{code}")
    @Transactional
    public AdminLanguageDto toggle(@PathVariable String code, @RequestBody LanguageToggleRequest request) {
        if (request == null || request.enabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите, включить ли язык");
        }
        Language language = languageRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Язык не найден"));
        if (language.isDefaultLanguage() && !request.enabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Основной язык выключить нельзя");
        }
        language.setEnabled(request.enabled());
        return toDto(language);
    }

    private AdminLanguageDto toDto(Language l) {
        return new AdminLanguageDto(l.getCode(), l.getName(), l.getNativeName(), l.isEnabled(), l.isDefaultLanguage(),
                l.isDefaultLanguage() ? 0 : translationRepository.countByLanguageCode(l.getCode()));
    }
}
