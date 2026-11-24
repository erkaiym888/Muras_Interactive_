package kg.murasinteractive.rating.service;

import kg.murasinteractive.awards.dto.LevelDto;
import kg.murasinteractive.awards.service.LevelService;
import kg.murasinteractive.points.repository.PointTransactionRepository;
import kg.murasinteractive.rating.dto.RatingDto;
import kg.murasinteractive.rating.dto.RatingRowDto;
import kg.murasinteractive.user.model.UserAccount;
import kg.murasinteractive.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Общий и недельный рейтинг всех зарегистрированных пользователей (FR-5.5, Приложение Б.4).
 * Общий - по total_points, недельный - по сумме point_transaction с понедельника 00:00 по Бишкеку.
 * Показывается только отображаемое имя; при равных очках - одно место (1, 1, 3).
 */
@Service
public class RatingService {

    public static final ZoneId BISHKEK = ZoneId.of("Asia/Bishkek");
    public static final String ALL = "all";
    public static final String WEEK = "week";
    private static final int LIMIT = 50;

    private final UserAccountRepository userRepository;
    private final PointTransactionRepository transactionRepository;
    private final LevelService levelService;

    public RatingService(UserAccountRepository userRepository, PointTransactionRepository transactionRepository,
                         LevelService levelService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.levelService = levelService;
    }

    /** Понедельник текущей недели по Бишкеку. */
    public static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional(readOnly = true)
    public RatingDto rating(String period, Long currentUserId) {
        String p = period == null || period.isBlank() ? ALL : period;
        if (!p.equals(ALL) && !p.equals(WEEK)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period: all или week");
        }
        LocalDate start = weekStart(LocalDate.now(BISHKEK));
        List<Entry> ranked = p.equals(WEEK) ? weekly(start) : overall();

        List<RatingRowDto> rows = new ArrayList<>();
        RatingRowDto mine = null;
        for (Entry e : ranked) {
            boolean me = Objects.equals(e.user.getUserId(), currentUserId);
            LevelDto level = levelService.levelFor(e.user.getTotalPoints());
            RatingRowDto row = new RatingRowDto(e.place, e.user.getDisplayName(), level.number(), level.title(),
                    e.points, me);
            if (rows.size() < LIMIT) rows.add(row);
            else if (me) mine = row;
        }
        return new RatingDto(p, p.equals(WEEK) ? start : null, p.equals(WEEK) ? start.plusDays(6) : null,
                ranked.size(), rows, mine);
    }

    /** Лидеры недели, начавшейся в weekStart: все с наибольшей положительной суммой. */
    @Transactional(readOnly = true)
    public List<Long> weekLeaders(LocalDate weekStart) {
        return weekly(weekStart).stream()
                .filter(e -> e.place == 1 && e.points > 0)
                .map(e -> e.user.getUserId())
                .toList();
    }

    // ---------- расчёт ----------

    private record Entry(UserAccount user, int points, int place) {
    }

    private List<Entry> overall() {
        Map<Long, Integer> points = new HashMap<>();
        List<UserAccount> users = userRepository.findAll();
        users.forEach(u -> points.put(u.getUserId(), u.getTotalPoints()));
        return rank(users, points);
    }

    private List<Entry> weekly(LocalDate weekStart) {
        Instant from = weekStart.atStartOfDay(BISHKEK).toInstant();
        Instant to = weekStart.plusDays(7).atStartOfDay(BISHKEK).toInstant();
        Map<Long, Integer> points = new HashMap<>();
        for (Object[] row : transactionRepository.sumByUserBetween(from, to)) {
            points.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return rank(userRepository.findAll(), points);
    }

    /** Сортировка по очкам; при равенстве раньше идёт тот, кто раньше зарегистрировался. Места 1, 1, 3. */
    private static List<Entry> rank(List<UserAccount> users, Map<Long, Integer> points) {
        List<UserAccount> sorted = new ArrayList<>(users);
        sorted.sort(Comparator.<UserAccount>comparingInt(u -> -points.getOrDefault(u.getUserId(), 0))
                .thenComparing(UserAccount::getRegisteredAt));
        List<Entry> result = new ArrayList<>();
        int place = 0, previous = Integer.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            int p = points.getOrDefault(sorted.get(i).getUserId(), 0);
            if (p != previous) {
                place = i + 1;
                previous = p;
            }
            result.add(new Entry(sorted.get(i), p, place));
        }
        return result;
    }
}
