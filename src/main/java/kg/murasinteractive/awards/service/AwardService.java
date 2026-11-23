package kg.murasinteractive.awards.service;

import kg.murasinteractive.awards.dto.AchievementDto;
import kg.murasinteractive.awards.dto.AwardsDto;
import kg.murasinteractive.awards.dto.LevelDto;
import kg.murasinteractive.awards.dto.RewardEventDto;
import kg.murasinteractive.awards.model.Achievement;
import kg.murasinteractive.awards.model.ContentView;
import kg.murasinteractive.awards.model.UserAchievement;
import kg.murasinteractive.awards.model.UserActivityDay;
import kg.murasinteractive.awards.repository.AchievementRepository;
import kg.murasinteractive.awards.repository.ContentViewRepository;
import kg.murasinteractive.awards.repository.UserAchievementRepository;
import kg.murasinteractive.awards.repository.UserActivityDayRepository;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import kg.murasinteractive.points.service.PointRules;
import kg.murasinteractive.points.service.PointService;
import kg.murasinteractive.quiz.dto.PointLineDto;
import kg.murasinteractive.quiz.model.QuizAttempt;
import kg.murasinteractive.quiz.model.QuizAttemptAnswer;
import kg.murasinteractive.quiz.model.QuizTopic;
import kg.murasinteractive.quiz.repository.QuizAttemptRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.repository.RiteRepository;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Награды (FR-5.3): значки Приложения Б.3, уровни Б.2 и очки за изучение материалов
 * и ежедневный вход (Б.1). Каждый значок выдаётся один раз, его бонус пишется в point_transaction.
 */
@Service
public class AwardService {

    public static final ZoneId BISHKEK = ZoneId.of("Asia/Bishkek");

    /** Ключи 15 элементов схемы юрты (design/src/yurt.ts): чужие ключи не принимаются. */
    public static final Set<String> YURT_PARTS = Set.of("chamgarak", "tunduk", "uuk", "tizgich", "kerege",
            "artkyUzuk", "astynkyUzuk", "tuurduk", "eshik", "chiy", "kyrchoo", "uuktunMatoosu", "tundukJabuu",
            "keregeChalgych", "jabykBash");

    private static final Set<String> CARD_TYPES = Set.of(ContentView.RITE, ContentView.HERO, ContentView.WORK);

    private final UserAccountRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserActivityDayRepository activityRepository;
    private final ContentViewRepository viewRepository;
    private final QuizAttemptRepository attemptRepository;
    private final RiteRepository riteRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;
    private final PointRules rules;
    private final PointService pointService;
    private final LevelService levelService;

    public AwardService(UserAccountRepository userRepository, AchievementRepository achievementRepository,
                        UserAchievementRepository userAchievementRepository, UserActivityDayRepository activityRepository,
                        ContentViewRepository viewRepository, QuizAttemptRepository attemptRepository,
                        RiteRepository riteRepository, FolkloreHeroRepository heroRepository,
                        FolkloreWorkRepository workRepository, PointRules rules, PointService pointService,
                        LevelService levelService) {
        this.userRepository = userRepository;
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.activityRepository = activityRepository;
        this.viewRepository = viewRepository;
        this.attemptRepository = attemptRepository;
        this.riteRepository = riteRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
        this.rules = rules;
        this.pointService = pointService;
        this.levelService = levelService;
    }

    // ---------- Сводка для кабинета ----------

    @Transactional(readOnly = true)
    public AwardsDto awards(Long userId) {
        UserAccount user = findUser(userId);
        int points = user.getTotalPoints();
        LevelDto level = levelService.levelFor(points);
        LevelDto next = levelService.nextLevel(points);
        int toNext = next == null ? 0 : next.minPoints() - points;
        int percent = next == null ? 100
                : (int) Math.round(100.0 * (points - level.minPoints()) / (next.minPoints() - level.minPoints()));

        Map<String, Instant> earned = new HashMap<>();
        for (UserAchievement ua : userAchievementRepository.findByUser_UserIdOrderByAwardedAtDesc(userId)) {
            earned.put(ua.getAchievement().getAchievementCode(), ua.getAwardedAt());
        }
        List<AchievementDto> achievements = achievementRepository.findAllByOrderBySortOrderAsc().stream()
                .map(a -> toDto(a, earned.get(a.getAchievementCode())))
                .toList();
        return new AwardsDto(points, level, next, toNext, percent, loginStreak(userId, today()),
                levelService.levels(), achievements);
    }

    /** Полученные значки, новые сверху - для личного кабинета. */
    @Transactional(readOnly = true)
    public List<AchievementDto> earned(Long userId) {
        return userAchievementRepository.findByUser_UserIdOrderByAwardedAtDesc(userId).stream()
                .map(ua -> toDto(ua.getAchievement(), ua.getAwardedAt()))
                .toList();
    }

    // ---------- Вход на сайт ----------

    /**
     * Вход на сайт: +5 раз в сутки, значок «Первый шаг» и серии входов 3 / 7 / 30 дней подряд.
     * Повторный вызов в тот же день ничего не начисляет.
     */
    @Transactional
    public RewardEventDto visit(Long userId) {
        UserAccount user = findUser(userId);
        Event event = new Event(user);
        LocalDate today = today();
        if (!activityRepository.existsByUser_UserIdAndActivityDate(userId, today)) {
            activityRepository.save(new UserActivityDay(user, today));
            event.points("DAILY_LOGIN", "Ежедневный вход", null, rules.points(PointRules.DAILY_LOGIN));
            event.achievement(AchievementCodes.FIRST_STEP);
            int streak = loginStreak(userId, today);
            if (streak >= 3) event.achievement(AchievementCodes.HEARTH_FIRE);
            if (streak >= 7) event.achievement(AchievementCodes.HEARTH_WEEK);
            if (streak >= 30) event.achievement(AchievementCodes.TRADITION_KEEPER);
        }
        return event.finish();
    }

    // ---------- Изучение материалов ----------

    /**
     * Открыт материал. Элемент юрты: +2 за первое открытие, значок «Хозяин юрты».
     * Карточка обряда, героя, произведения: +3 за первое прочтение (не больше 30 очков в сутки),
     * значки «Знаток обрядов» и «Сказитель». Переход из поиска: значок «Следопыт».
     */
    @Transactional
    public RewardEventDto view(Long userId, String type, String key) {
        UserAccount user = findUser(userId);
        String cleanKey = key.trim();
        validate(type, cleanKey);
        Event event = new Event(user);
        if (viewRepository.existsByUser_UserIdAndItemTypeAndItemKey(userId, type, cleanKey)) {
            return event.finish();
        }
        long readToday = viewRepository.countByUser_UserIdAndItemTypeInAndViewedAtGreaterThanEqual(
                userId, CARD_TYPES, startOfToday());
        viewRepository.save(new ContentView(user, type, cleanKey));

        switch (type) {
            case ContentView.YURT_PART -> {
                event.points("YURT_ELEMENT_FIRST", "Первое открытие элемента юрты", null,
                        rules.points(PointRules.YURT_ELEMENT_FIRST));
                Set<String> opened = viewRepository.findByUser_UserIdAndItemType(userId, ContentView.YURT_PART)
                        .stream().map(ContentView::getItemKey).collect(Collectors.toSet());
                if (opened.containsAll(YURT_PARTS)) event.achievement(AchievementCodes.YURT_MASTER);
            }
            case ContentView.RITE, ContentView.HERO, ContentView.WORK -> {
                int perCard = rules.points(PointRules.CARD_FIRST_READ);
                int limit = rules.points(PointRules.STUDY_DAILY_LIMIT);
                if (perCard > 0 && (readToday + 1) * perCard <= limit) {
                    event.points("CARD_FIRST_READ", "Первое прочтение карточки", Long.valueOf(cleanKey), perCard);
                } else {
                    event.info("CARD_FIRST_READ", "Лимит очков за изучение материалов на сегодня исчерпан");
                }
                if (type.equals(ContentView.RITE) && readAllRiteCategories(userId)) {
                    event.achievement(AchievementCodes.RITES_EXPERT);
                }
                if (!type.equals(ContentView.RITE) && viewRepository.countByUser_UserIdAndItemTypeIn(
                        userId, Set.of(ContentView.HERO, ContentView.WORK)) >= 10) {
                    event.achievement(AchievementCodes.STORYTELLER);
                }
            }
            case ContentView.SEARCH -> {
                if (viewRepository.countByUser_UserIdAndItemTypeIn(userId, Set.of(ContentView.SEARCH)) >= 10) {
                    event.achievement(AchievementCodes.PATHFINDER);
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный тип материала");
        }
        return event.finish();
    }

    // ---------- Викторины ----------

    /** Проверка значков после прохождения викторины. Очки за саму викторину уже начислены. */
    @Transactional
    public RewardEventDto afterQuiz(UserAccount user, QuizAttempt attempt, int pointsBefore) {
        Event event = new Event(user, pointsBefore);
        List<QuizAttempt> all = attemptRepository.findByUser_UserIdOrderByCompletedAtDesc(user.getUserId());

        event.achievement(AchievementCodes.QUEST_START);
        if (attempt.getScore() >= 100 && attempt.getQuestionCount() >= 5) event.achievement(AchievementCodes.FLAWLESS);
        if (attempt.getScore() >= 80 && attempt.getHintsUsed() == 0) event.achievement(AchievementCodes.NO_HINTS);

        long strongQuizzes = all.stream().filter(a -> a.getScore() >= 80)
                .map(a -> a.getQuiz().getQuizId()).distinct().count();
        if (strongQuizzes >= 5) event.achievement(AchievementCodes.ERUDITE);

        Set<QuizTopic> topics = EnumSet.noneOf(QuizTopic.class);
        all.stream().filter(a -> a.getScore() >= 70).forEach(a -> topics.add(a.getQuiz().getTopic()));
        if (topics.containsAll(EnumSet.allOf(QuizTopic.class))) event.achievement(AchievementCodes.THREE_TOPICS);

        if (longestCorrectRun(all) >= 10) event.achievement(AchievementCodes.SHARP_EYE);
        return event.finish();
    }

    /** Значок «Лидер недели» (FR-5.5): вызывается еженедельной задачей рейтинга. */
    @Transactional
    public RewardEventDto awardWeekLeader(Long userId) {
        Event event = new Event(findUser(userId));
        event.achievement(AchievementCodes.WEEK_LEADER);
        return event.finish();
    }

    // ---------- вспомогательное ----------

    /** Самая длинная серия верных ответов подряд по всем прохождениям в порядке времени. */
    private static int longestCorrectRun(List<QuizAttempt> attemptsNewestFirst) {
        List<QuizAttempt> ordered = new ArrayList<>(attemptsNewestFirst);
        ordered.sort(Comparator.comparing(QuizAttempt::getCompletedAt).thenComparing(QuizAttempt::getAttemptId));
        int run = 0, best = 0;
        for (QuizAttempt a : ordered) {
            for (QuizAttemptAnswer answer : a.getAnswers()) {
                run = answer.isCorrect() ? run + 1 : 0;
                best = Math.max(best, run);
            }
        }
        return best;
    }

    /** Прочитана хотя бы одна карточка в каждой категории, где есть опубликованные обряды. */
    private boolean readAllRiteCategories(Long userId) {
        Set<Long> readIds = viewRepository.findByUser_UserIdAndItemType(userId, ContentView.RITE).stream()
                .map(v -> parseId(v.getItemKey())).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Boolean> categories = new HashMap<>();
        for (Rite rite : riteRepository.findByStatusOrderByRiteIdAsc(Rite.STATUS_PUBLISHED)) {
            Long category = rite.getRiteCategory().getRiteCategoryId();
            categories.merge(category, readIds.contains(rite.getRiteId()), Boolean::logicalOr);
        }
        return !categories.isEmpty() && !categories.containsValue(false);
    }

    /** Ключ должен указывать на существующий материал - иначе очки можно было бы «накрутить». */
    private void validate(String type, String key) {
        boolean ok = switch (type) {
            case ContentView.YURT_PART -> YURT_PARTS.contains(key);
            case ContentView.RITE -> exists(key, id -> riteRepository.findByRiteIdAndStatus(id, Rite.STATUS_PUBLISHED).isPresent());
            case ContentView.HERO -> exists(key, heroRepository::existsById);
            case ContentView.WORK -> exists(key, workRepository::existsById);
            case ContentView.SEARCH -> key.length() >= 2;
            default -> false;
        };
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Материал не найден");
        }
    }

    private static boolean exists(String key, java.util.function.Predicate<Long> check) {
        Long id = parseId(key);
        return id != null && check.test(id);
    }

    private static Long parseId(String key) {
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Сколько дней подряд, включая сегодняшний, пользователь заходил на сайт. */
    private int loginStreak(Long userId, LocalDate today) {
        Set<LocalDate> days = new HashSet<>();
        activityRepository.findByUser_UserIdOrderByActivityDateDesc(userId).forEach(d -> days.add(d.getActivityDate()));
        int streak = 0;
        LocalDate day = today;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static LocalDate today() {
        return LocalDate.now(BISHKEK);
    }

    private static Instant startOfToday() {
        return today().atStartOfDay(BISHKEK).toInstant();
    }

    private static AchievementDto toDto(Achievement a, Instant awardedAt) {
        return new AchievementDto(a.getAchievementCode(), a.getTitle(), a.getGroupName(),
                a.getConditionDescription(), a.getPointsBonus(), a.getIconPath(), awardedAt != null, awardedAt);
    }

    /** Собирает начисления одного события и в конце определяет, был ли переход на новый уровень. */
    private final class Event {
        private final UserAccount user;
        private final int pointsBefore;
        private final List<PointLineDto> lines = new ArrayList<>();
        private final List<AchievementDto> newAchievements = new ArrayList<>();

        Event(UserAccount user) {
            this(user, user.getTotalPoints());
        }

        Event(UserAccount user, int pointsBefore) {
            this.user = user;
            this.pointsBefore = pointsBefore;
        }

        void points(String code, String label, Long sourceId, int points) {
            if (points <= 0) return;
            pointService.award(user, code, sourceId, points);
            lines.add(new PointLineDto(code, label, points));
        }

        void info(String code, String label) {
            lines.add(new PointLineDto(code, label, 0));
        }

        void achievement(String code) {
            if (userAchievementRepository.existsByUser_UserIdAndAchievement_AchievementCode(user.getUserId(), code)) {
                return;
            }
            Achievement a = achievementRepository.findByAchievementCode(code).orElse(null);
            if (a == null) return;
            UserAchievement ua = userAchievementRepository.save(new UserAchievement(user, a));
            pointService.award(user, "ACHIEVEMENT", a.getAchievementId(), a.getPointsBonus());
            lines.add(new PointLineDto("ACHIEVEMENT", "Значок «" + a.getTitle() + "»", a.getPointsBonus()));
            newAchievements.add(toDto(a, ua.getAwardedAt()));
        }

        RewardEventDto finish() {
            int total = user.getTotalPoints();
            LevelDto before = levelService.levelFor(pointsBefore);
            LevelDto after = levelService.levelFor(total);
            int earned = lines.stream().mapToInt(PointLineDto::points).sum();
            return new RewardEventDto(lines, earned, total, newAchievements,
                    after.number() > before.number() ? after : null);
        }
    }
}
