package kg.murasinteractive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Включает задачи по расписанию (например, значок «Лидер недели», FR-5.5). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
