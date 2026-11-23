package kg.murasinteractive.i18n.controller;

import kg.murasinteractive.i18n.dto.I18nDtos.*;
import kg.murasinteractive.i18n.service.TranslationEditorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Переводы материалов в редакторе контента (FR-8.1), роли EDITOR и ADMIN (/api/editor/**).
 *  GET /api/editor/translations                        - виды материалов и языки перевода
 *  GET /api/editor/translations/{type}?lang=ky         - материалы вида и прогресс перевода
 *  GET /api/editor/translations/{type}/{id}?lang=ky    - оригинал и перевод по полям
 *  PUT /api/editor/translations/{type}/{id}?lang=ky    - сохранить ({"fields": {"title": "..."}})
 */
@RestController
@RequestMapping("/api/editor/translations")
public class TranslationEditorController {

    private final TranslationEditorService service;

    public TranslationEditorController(TranslationEditorService service) {
        this.service = service;
    }

    @GetMapping
    public TranslationMetaDto meta() {
        return service.meta();
    }

    @GetMapping("/{type}")
    public List<TranslationItemDto> items(@PathVariable String type, @RequestParam String lang) {
        return service.items(type, lang);
    }

    @GetMapping("/{type}/{id}")
    public TranslationFormDto form(@PathVariable String type, @PathVariable Long id, @RequestParam String lang) {
        return service.form(type, id, lang);
    }

    @PutMapping("/{type}/{id}")
    public TranslationFormDto save(@PathVariable String type, @PathVariable Long id, @RequestParam String lang,
                                   @RequestBody TranslationSaveRequest request) {
        return service.save(type, id, lang, request.fields());
    }
}
