package kg.murasinteractive.awards.service;

import kg.murasinteractive.awards.model.Achievement;
import kg.murasinteractive.awards.model.UserLevel;
import kg.murasinteractive.awards.repository.AchievementRepository;
import kg.murasinteractive.awards.repository.UserLevelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Справочники наград по Приложению Б ТЗ: 8 уровней (Б.2) и 15 значков (Б.3).
 * Добавляются только отсутствующие записи - изменения администратора в базе не затираются.
 */
@Component
public class AwardsSeeder implements CommandLineRunner {

    private final UserLevelRepository levelRepository;
    private final AchievementRepository achievementRepository;

    public AwardsSeeder(UserLevelRepository levelRepository, AchievementRepository achievementRepository) {
        this.levelRepository = levelRepository;
        this.achievementRepository = achievementRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<UserLevel> levels = List.of(
                new UserLevel(1, "Конок (Гость)", 0, "Стартовая рамка аватара"),
                new UserLevel(2, "Шакирт (Ученик)", 100, "Рамка профиля «Кочкор мүйүз»"),
                new UserLevel(3, "Изилдөөчү (Исследователь)", 250, "Рамка профиля «Тумар»"),
                new UserLevel(4, "Чебер (Мастер)", 500, "Рамка профиля «Ай гүл»"),
                new UserLevel(5, "Акын", 800, "Открываются викторины повышенной сложности"),
                new UserLevel(6, "Бий", 1200, "Золотая рамка профиля"),
                new UserLevel(7, "Аксакал", 1700, "Серебряная рамка с орнаментом и звание в рейтинге"),
                new UserLevel(8, "Манасчы", 2300, "Высшее звание и особая золотая рамка"));
        for (UserLevel level : levels) {
            if (levelRepository.findByLevelNumber(level.getLevelNumber()).isEmpty()) {
                levelRepository.save(level);
            }
        }

        int order = 1;
        List<Achievement> achievements = List.of(
                new Achievement(AchievementCodes.FIRST_STEP, "Первый шаг", "Исследование", "Первый вход после регистрации", 10, order++),
                new Achievement(AchievementCodes.YURT_MASTER, "Хозяин юрты", "Исследование", "Открыты все элементы схемы юрты", 50, order++),
                new Achievement(AchievementCodes.RITES_EXPERT, "Знаток обрядов", "Исследование", "Прочитана хотя бы одна карточка в каждой категории обрядов", 30, order++),
                new Achievement(AchievementCodes.STORYTELLER, "Сказитель", "Исследование", "Прочитаны 10 карточек героев и произведений", 30, order++),
                new Achievement(AchievementCodes.PATHFINDER, "Следопыт", "Исследование", "10 поисковых запросов с переходом к результату", 10, order++),
                new Achievement(AchievementCodes.QUEST_START, "Начало квеста", "Викторины", "Завершена первая викторина", 20, order++),
                new Achievement(AchievementCodes.FLAWLESS, "Безупречно", "Викторины", "100% в викторине из 5 и более вопросов", 50, order++),
                new Achievement(AchievementCodes.SHARP_EYE, "Меткий глаз", "Викторины", "10 верных ответов подряд", 30, order++),
                new Achievement(AchievementCodes.ERUDITE, "Эрудит", "Викторины", "5 разных викторин с результатом 80% и выше", 100, order++),
                new Achievement(AchievementCodes.THREE_TOPICS, "Знаток трёх тем", "Викторины", "По одной викторине на каждую тему (юрта, обряды, герои) с результатом от 70%", 80, order++),
                new Achievement(AchievementCodes.NO_HINTS, "Без подсказок", "Викторины", "Викторина пройдена на 80% и выше без подсказок", 20, order++),
                new Achievement(AchievementCodes.HEARTH_FIRE, "Огонь очага", "Постоянство", "Вход 3 дня подряд", 15, order++),
                new Achievement(AchievementCodes.HEARTH_WEEK, "Неделя у очага", "Постоянство", "Вход 7 дней подряд", 50, order++),
                new Achievement(AchievementCodes.TRADITION_KEEPER, "Хранитель традиций", "Постоянство", "Вход 30 дней подряд", 200, order++),
                new Achievement(AchievementCodes.WEEK_LEADER, "Лидер недели", "Рейтинг", "Первое место в недельном рейтинге", 100, order));
        for (Achievement a : achievements) {
            if (achievementRepository.findByAchievementCode(a.getAchievementCode()).isEmpty()) {
                achievementRepository.save(a);
            }
        }
    }
}
