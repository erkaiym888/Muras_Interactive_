package kg.murasinteractive.points.service;

import kg.murasinteractive.points.model.PointRule;
import kg.murasinteractive.points.repository.PointRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Правила начисления очков (Приложение Б.1). Значения по умолчанию из ТЗ записываются в таблицу
 * point_rule при первом запуске; дальше они читаются из базы, и администратор может их менять.
 */
@Service
public class PointRules implements CommandLineRunner {

    public static final String HINT_MULTIPLIER = "QUIZ_HINT_MULTIPLIER";
    public static final String REPEAT_MULTIPLIER = "QUIZ_REPEAT_MULTIPLIER";
    public static final String STREAK_3 = "QUIZ_STREAK_3";
    public static final String STREAK_5 = "QUIZ_STREAK_5";
    public static final String COMPLETE = "QUIZ_COMPLETE";
    public static final String COMPLETE_DAILY_LIMIT = "QUIZ_COMPLETE_DAILY_LIMIT";
    public static final String RESULT_80 = "QUIZ_RESULT_80";
    public static final String RESULT_100 = "QUIZ_RESULT_100";
    public static final String FIRST_PASS = "QUIZ_FIRST_PASS";

    /** Значения из ТЗ: код -> {значение, описание}. */
    private static final Map<String, Object[]> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put(HINT_MULTIPLIER, new Object[]{0.5, "Использование подсказки: очки за вопрос умножаются на это значение"});
        DEFAULTS.put(REPEAT_MULTIPLIER, new Object[]{0.5, "Повторное прохождение: очки за вопросы умножаются на это значение"});
        DEFAULTS.put(STREAK_3, new Object[]{5.0, "Серия из 3 верных ответов подряд (один раз за прохождение)"});
        DEFAULTS.put(STREAK_5, new Object[]{10.0, "Серия из 5 и более верных ответов подряд (вместо бонуса за 3)"});
        DEFAULTS.put(COMPLETE, new Object[]{20.0, "Завершение викторины"});
        DEFAULTS.put(COMPLETE_DAILY_LIMIT, new Object[]{3.0, "Сколько раз в сутки на одну викторину начисляется бонус за завершение"});
        DEFAULTS.put(RESULT_80, new Object[]{20.0, "Результат 80% и выше (только первое прохождение)"});
        DEFAULTS.put(RESULT_100, new Object[]{50.0, "Результат 100%, заменяет бонус за 80% (только первое прохождение)"});
        DEFAULTS.put(FIRST_PASS, new Object[]{25.0, "Первое прохождение викторины"});
    }

    private final PointRuleRepository repository;

    public PointRules(PointRuleRepository repository) {
        this.repository = repository;
    }

    /** Добавляет недостающие правила со значениями из ТЗ; уже изменённые значения не трогает. */
    @Override
    @Transactional
    public void run(String... args) {
        DEFAULTS.forEach((code, v) -> {
            if (!repository.existsById(code)) {
                repository.save(new PointRule(code, (Double) v[0], (String) v[1]));
            }
        });
    }

    /** Значение правила из базы; если строки нет - значение из ТЗ. */
    @Transactional(readOnly = true)
    public double value(String code) {
        return repository.findById(code)
                .map(PointRule::getRuleValue)
                .orElseGet(() -> (Double) DEFAULTS.get(code)[0]);
    }

    public int points(String code) {
        return (int) Math.round(value(code));
    }
}
