package kg.murasinteractive.quiz.service;

import kg.murasinteractive.quiz.dto.*;
import kg.murasinteractive.quiz.model.AnswerOption;
import kg.murasinteractive.quiz.model.Quiz;
import kg.murasinteractive.quiz.model.QuestionType;
import kg.murasinteractive.quiz.model.QuizQuestion;
import kg.murasinteractive.quiz.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class QuizService {

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryDto> findQuizzes() {
        return quizRepository.findAllByOrderByQuizIdAsc().stream()
                .map(q -> new QuizSummaryDto(q.getQuizId(), q.getTitle(), q.getTopic().name(),
                        q.getDifficulty().name(), q.getDescription(), q.getQuestions().size()))
                .toList();
    }

    /** Викторина для прохождения: вопросы и варианты без правильных ответов. */
    @Transactional(readOnly = true)
    public QuizDto getQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Викторина не найдена"));
        List<QuizQuestionDto> questions = quiz.getQuestions().stream().map(this::toDto).toList();
        return new QuizDto(quiz.getQuizId(), quiz.getTitle(), quiz.getTopic().name(),
                quiz.getDifficulty().name(), quiz.getDescription(), questions);
    }

    private QuizQuestionDto toDto(QuizQuestion question) {
        List<QuizOptionDto> options = question.getOptions().stream()
                .map(o -> new QuizOptionDto(o.getAnswerOptionId(), o.getOptionText()))
                .toList();
        List<String> matchChoices = new ArrayList<>();
        if (question.getQuestionType() == QuestionType.MATCHING) {
            for (AnswerOption option : question.getOptions()) {
                matchChoices.add(option.getMatchText());
            }
            // порядок правых частей перемешиваем, иначе он выдал бы ответ
            Collections.shuffle(matchChoices);
        }
        return new QuizQuestionDto(question.getQuestionId(), question.getQuestionType().name(),
                question.getQuestionText(), question.getPoints(), options, matchChoices);
    }
}
