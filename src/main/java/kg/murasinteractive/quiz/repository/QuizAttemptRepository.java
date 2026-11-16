package kg.murasinteractive.quiz.repository;

import kg.murasinteractive.quiz.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Проходил ли пользователь эту викторину раньше (тогда это повторное прохождение). */
    boolean existsByUser_UserIdAndQuiz_QuizId(Long userId, Long quizId);

    /** Сколько раз викторина завершена с начала суток - для лимита бонуса за завершение. */
    long countByUser_UserIdAndQuiz_QuizIdAndCompletedAtGreaterThanEqual(Long userId, Long quizId, Instant from);
}
