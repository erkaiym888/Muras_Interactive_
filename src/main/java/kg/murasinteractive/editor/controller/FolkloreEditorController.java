package kg.murasinteractive.editor.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.editor.dto.FolkloreHeroForm;
import kg.murasinteractive.editor.dto.FolkloreRow;
import kg.murasinteractive.editor.dto.FolkloreWorkForm;
import kg.murasinteractive.editor.service.FolkloreEditorService;
import kg.murasinteractive.folklore.dto.FolkloreTypeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Управление фольклором (FR-7.1). Доступ только для ролей EDITOR и ADMIN (SecurityConfig: /api/editor/**).
 *  GET    /api/editor/folklore              - все герои и произведения
 *  GET    /api/editor/folklore/types        - типы фольклора
 *  GET    /api/editor/folklore/heroes/{id}  - герой для формы;  POST /heroes, PUT /heroes/{id}, DELETE /heroes/{id}
 *  GET    /api/editor/folklore/works/{id}   - произведение;     POST /works,  PUT /works/{id},  DELETE /works/{id}
 */
@RestController
@RequestMapping("/api/editor/folklore")
public class FolkloreEditorController {

    private final FolkloreEditorService editorService;

    public FolkloreEditorController(FolkloreEditorService editorService) {
        this.editorService = editorService;
    }

    @GetMapping
    public List<FolkloreRow> list() {
        return editorService.list();
    }

    @GetMapping("/types")
    public List<FolkloreTypeDto> types() {
        return editorService.types();
    }

    @GetMapping("/heroes/{heroId}")
    public FolkloreHeroForm hero(@PathVariable Long heroId) {
        return editorService.getHero(heroId);
    }

    @PostMapping("/heroes")
    public ResponseEntity<FolkloreHeroForm> createHero(@Valid @RequestBody FolkloreHeroForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(editorService.createHero(form));
    }

    @PutMapping("/heroes/{heroId}")
    public FolkloreHeroForm updateHero(@PathVariable Long heroId, @Valid @RequestBody FolkloreHeroForm form) {
        return editorService.updateHero(heroId, form);
    }

    @DeleteMapping("/heroes/{heroId}")
    public ResponseEntity<Void> deleteHero(@PathVariable Long heroId) {
        editorService.deleteHero(heroId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/works/{workId}")
    public FolkloreWorkForm work(@PathVariable Long workId) {
        return editorService.getWork(workId);
    }

    @PostMapping("/works")
    public ResponseEntity<FolkloreWorkForm> createWork(@Valid @RequestBody FolkloreWorkForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(editorService.createWork(form));
    }

    @PutMapping("/works/{workId}")
    public FolkloreWorkForm updateWork(@PathVariable Long workId, @Valid @RequestBody FolkloreWorkForm form) {
        return editorService.updateWork(workId, form);
    }

    @DeleteMapping("/works/{workId}")
    public ResponseEntity<Void> deleteWork(@PathVariable Long workId) {
        editorService.deleteWork(workId);
        return ResponseEntity.noContent().build();
    }
}
