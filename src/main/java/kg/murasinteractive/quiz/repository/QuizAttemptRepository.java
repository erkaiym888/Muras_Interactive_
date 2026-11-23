package kg.murasinteractive.quiz.repository;

import kg.murasinteractive.quiz.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Проходил ли пользователь эту викторину раньше (тогда это повторное прохождение). */
    boolean existsByUser_UserIdAndQuiz_QuizId(Long userId, Long quizId);

    /** Сколько раз викторина завершена с начала суток - для лимита бонуса за завершение. */
    long countByUser_UserIdAndQuiz_QuizIdAndCompletedAtGreaterThanEqual(Long userId, Long quizId, Instant from);

    /** История прохождений пользователя, новые сверху (FR-5.4). */
    List<QuizAttempt> findByUser_UserIdOrderByCompletedAtDesc(Long userId);

    /** Прохождение только своего пользователя: чужое не отдаём. */
    Optional<QuizAttempt> findByAttemptIdAndUser_UserId(Long attemptId, Long userId);
}
