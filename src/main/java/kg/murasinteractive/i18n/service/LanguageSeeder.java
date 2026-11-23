package kg.murasinteractive.i18n.service;

import kg.murasinteractive.i18n.model.Language;
import kg.murasinteractive.i18n.repository.LanguageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Начальный справочник языков (FR-8.1): русский включён, кыргызский и английский заведены, но выключены.
 * Включает их администратор в разделе «Пользователи → Языки», когда готовы переводы. Существующие строки не трогаем.
 */
@Component
public class LanguageSeeder implements CommandLineRunner {

    private final LanguageRepository languageRepository;

    public LanguageSeeder(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        add(new Language("ru", "Русский", "Русский", true, true, 1));
        add(new Language("ky", "Кыргызский", "Кыргызча", false, false, 2));
        add(new Language("en", "Английский", "English", false, false, 3));
    }

    private void add(Language language) {
        if (!languageRepository.existsById(language.getCode())) {
            languageRepository.save(language);
        }
    }
}
