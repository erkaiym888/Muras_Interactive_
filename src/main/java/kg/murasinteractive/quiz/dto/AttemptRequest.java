package kg.murasinteractive.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Ответы на викторину (FR-5.2).
 * optionIds - выбранные варианты (один ответ, несколько ответов, верно/неверно);
 * pairs - для сопоставления: id левой части -> выбранная правая часть;
 * hintUsed - бралась ли подсказка (очки за вопрос уменьшаются вдвое).
 */
public record AttemptRequest(@NotNull @Valid List<Answer> answers) {

    public record Answer(@NotNull Long questionId, List<Long> optionIds, Map<Long, String> pairs, boolean hintUsed) {
    }
}
