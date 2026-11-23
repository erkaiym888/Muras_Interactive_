package kg.murasinteractive.relation.dto;

import java.util.List;

/** Связи материала с другими разделами (FR-3.3), сгруппированные по видам. */
public record RelationsDto(RelationNodeDto center,
                           List<RelationNodeDto> rites,
                           List<RelationNodeDto> heroes,
                           List<RelationNodeDto> works,
                           List<RelationNodeDto> yurtElements) {
}
