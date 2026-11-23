package kg.murasinteractive.quiz.dto;

import java.util.List;

/**
 * Подсказка к вопросу. Для выбора ответа - какие неверные варианты убрать,
 * для сопоставления - одна правильная пара. Для «верно / неверно» подсказки нет (available = false).
 */
public record HintDto(boolean available, String text, List<Long> hiddenOptionIds, List<Pair> revealedPairs) {

    public record Pair(Long optionId, String match) {
    }
}
