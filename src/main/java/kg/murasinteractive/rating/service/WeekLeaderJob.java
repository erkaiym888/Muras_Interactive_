package kg.murasinteractive.rating.service;

import kg.murasinteractive.awards.service.AwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Значок «Лидер недели» (Приложение Б.3): в понедельник в 00:05 по Бишкеку первое место
 * прошедшей недели получает значок. При запуске сервера проверка повторяется - на случай,
 * если сервер был выключен в понедельник. Значок выдаётся один раз, повторная проверка безопасна.
 */
@Component
public class WeekLeaderJob {

    private static final Logger log = LoggerFactory.getLogger(WeekLeaderJob.class);

    private final RatingService ratingService;
    private final AwardService awardService;

    public WeekLeaderJob(RatingService ratingService, AwardService awardService) {
        this.ratingService = ratingService;
        this.awardService = awardService;
    }

    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Bishkek")
    public void weekly() {
        awardPreviousWeek();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        awardPreviousWeek();
    }

    void awardPreviousWeek() {
        LocalDate lastWeek = RatingService.weekStart(LocalDate.now(RatingService.BISHKEK)).minusWeeks(1);
        for (Long userId : ratingService.weekLeaders(lastWeek)) {
            if (!awardService.awardWeekLeader(userId).newAchievements().isEmpty()) {
                log.info("Значок «Лидер недели» за неделю с {} выдан пользователю {}", lastWeek, userId);
            }
        }
    }
}
