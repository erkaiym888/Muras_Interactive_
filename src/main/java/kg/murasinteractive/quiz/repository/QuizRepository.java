package kg.murasinteractive.quiz.repository;

import kg.murasinteractive.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByOrderByQuizIdAsc();
}
