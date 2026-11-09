package kg.murasinteractive.editor.controller;

import jakarta.validation.Valid;
import kg.murasinteractive.editor.dto.CategoryForm;
import kg.murasinteractive.editor.dto.CategoryRow;
import kg.murasinteractive.editor.dto.RiteForm;
import kg.murasinteractive.editor.dto.RiteRow;
import kg.murasinteractive.editor.service.ContentEditorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API редактора контента (FR-7.1). Доступ только для ролей EDITOR и ADMIN (SecurityConfig).
 *  GET    /api/editor/rites                 - все обряды (черновики, опубликованные, скрытые)
 *  GET    /api/editor/rites/{id}            - обряд для формы редактирования
 *  POST   /api/editor/rites                 - добавить обряд
 *  PUT    /api/editor/rites/{id}            - изменить обряд
 *  DELETE /api/editor/rites/{id}            - удалить обряд
 *  GET    /api/editor/rite-categories       - категории с числом обрядов
 *  POST   /api/editor/rite-categories       - добавить категорию
 *  PUT    /api/editor/rite-categories/{id}  - переименовать категорию
 *  DELETE /api/editor/rite-categories/{id}  - удалить пустую категорию
 */
@RestController
@RequestMapping("/api/editor")
public class ContentEditorController {

    private final ContentEditorService editorService;

    public ContentEditorController(ContentEditorService editorService) {
        this.editorService = editorService;
    }

    @GetMapping("/rites")
    public List<RiteRow> rites() {
        return editorService.listRites();
    }

    @GetMapping("/rites/{riteId}")
    public RiteForm rite(@PathVariable Long riteId) {
        return editorService.getRite(riteId);
    }

    @PostMapping("/rites")
    public ResponseEntity<RiteForm> createRite(@Valid @RequestBody RiteForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(editorService.createRite(form));
    }

    @PutMapping("/rites/{riteId}")
    public RiteForm updateRite(@PathVariable Long riteId, @Valid @RequestBody RiteForm form) {
        return editorService.updateRite(riteId, form);
    }

    @DeleteMapping("/rites/{riteId}")
    public ResponseEntity<Void> deleteRite(@PathVariable Long riteId) {
        editorService.deleteRite(riteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rite-categories")
    public List<CategoryRow> categories() {
        return editorService.listCategories();
    }

    @PostMapping("/rite-categories")
    public ResponseEntity<CategoryRow> createCategory(@Valid @RequestBody CategoryForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(editorService.createCategory(form.name()));
    }

    @PutMapping("/rite-categories/{categoryId}")
    public CategoryRow renameCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryForm form) {
        return editorService.renameCategory(categoryId, form.name());
    }

    @DeleteMapping("/rite-categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        editorService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
