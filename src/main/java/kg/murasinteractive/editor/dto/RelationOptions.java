package kg.murasinteractive.editor.dto;

import java.util.List;

/** Варианты для блока «Связи» в форме обряда (FR-3.3). */
public record RelationOptions(List<Option> yurtElements, List<Option> heroes, List<Option> works) {

    /** key - ключ элемента юрты, id - идентификатор героя или произведения. */
    public record Option(Long id, String key, String title, String subtitle) {}
}
