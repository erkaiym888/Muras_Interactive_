package kg.murasinteractive.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Расширение PostgreSQL fuzzystrmatch (функция levenshtein) для поиска с опечатками (FR-4.2).
 * Оно входит в стандартную поставку PostgreSQL и с версии 13 подключается владельцем базы без прав суперпользователя.
 * Если подключить не удалось, поиск работает как раньше, только без исправления опечаток.
 */
@Component
public class FuzzySetup {

    private static final Logger log = LoggerFactory.getLogger(FuzzySetup.class);

    private final JdbcTemplate jdbc;
    private volatile boolean available;

    public FuzzySetup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            jdbc.execute("create extension if not exists fuzzystrmatch");
            available = true;
        } catch (Exception e) {
            available = false;
            log.warn("Поиск с опечатками выключен: не удалось подключить расширение fuzzystrmatch ({})."
                    + " Выполните в базе от имени владельца: CREATE EXTENSION fuzzystrmatch;", e.getMessage());
        }
    }

    public boolean available() {
        return available;
    }
}
