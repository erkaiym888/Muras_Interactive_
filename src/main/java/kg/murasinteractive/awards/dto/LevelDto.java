package kg.murasinteractive.awards.dto;

/** Уровень: номер, название, порог очков и награда за переход. */
public record LevelDto(int number, String title, int minPoints, String reward) {
}
