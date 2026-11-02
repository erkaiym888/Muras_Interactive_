package kg.murasinteractive.config;

import kg.murasinteractive.media.model.MediaFile;
import kg.murasinteractive.media.repository.MediaFileRepository;
import kg.murasinteractive.rite.model.Rite;
import kg.murasinteractive.rite.model.RiteCategory;
import kg.murasinteractive.rite.repository.RiteCategoryRepository;
import kg.murasinteractive.rite.repository.RiteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Заполняет пустую базу категориями и обрядами из макета Figma.
 * Тексты - заготовки: перед защитой замените их на проверенные материалы
 * с указанием настоящих источников.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String IMG_HERO =
            "https://images.unsplash.com/photo-1792922386215-9fec611e3e42?auto=format&fit=crop&w=1800&q=88";
    private static final String IMG_VALLEY =
            "https://images.unsplash.com/photo-1792922386261-5952ec998715?auto=format&fit=crop&w=1400&q=85";
    private static final String IMG_INTERIOR =
            "https://images.unsplash.com/photo-1489421382202-f7ec0cfd96f7?auto=format&fit=crop&w=1200&q=85";
    private static final String IMG_PEOPLE =
            "https://images.unsplash.com/photo-1621425022500-d801b0be1a05?auto=format&fit=crop&w=1200&q=85";

    private static final String TEST_SOURCES =
            "Тестовые данные. Заменить на реальные источники (книги, статьи, этнографические работы).";

    private final RiteCategoryRepository categoryRepository;
    private final RiteRepository riteRepository;
    private final MediaFileRepository mediaFileRepository;

    public DataSeeder(RiteCategoryRepository categoryRepository, RiteRepository riteRepository,
                      MediaFileRepository mediaFileRepository) {
        this.categoryRepository = categoryRepository;
        this.riteRepository = riteRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            for (String name : List.of("Свадебные", "Рождение ребёнка",
                    "Похоронные и поминальные", "Праздничные", "Бытовые")) {
                categoryRepository.save(new RiteCategory(name));
            }
        }
        if (riteRepository.count() > 0) {
            return;
        }

        Rite beshik = newRite("Рождение ребёнка", "Бешик той",
                "Семейный обряд первого укладывания младенца в традиционную колыбель — бешик.",
                "Бешик той — семейное торжество, посвящённое первому укладыванию младенца в бешик, "
                        + "традиционную кыргызскую колыбель.\n\n"
                        + "Обряд обычно проводит уважаемая старшая женщина семьи. Родные собираются, "
                        + "чтобы пожелать ребёнку здоровья, долгой жизни и достойного пути.",
                "Бешик воспринимается не только как предмет быта, но и как защищённое первое "
                        + "пространство ребёнка. Обряд подчёркивает участие рода в заботе о новом поколении.",
                "Материалы этнографических исследований Кыргызского национального музея и открытых "
                        + "публикаций о семейной обрядности кыргызов.",
                IMG_INTERIOR);
        beshik.addStep(1, "Подготовка бешика",
                "Колыбель очищают, украшают и застилают специально подготовленными принадлежностями.");
        beshik.addStep(2, "Алас-алас",
                "Проводится символическое очищение дымом арчи с добрыми пожеланиями.");
        beshik.addStep(3, "Укладывание ребёнка",
                "Опытная женщина показывает молодой матери, как безопасно уложить младенца.");
        beshik.addStep(4, "Бата",
                "Старшие произносят благословение, после чего семье преподносят подарки.");

        Rite tushoo = newRite("Рождение ребёнка", "Тушоо кесүү",
                "Праздник первых шагов ребёнка и символическое разрезание пут.",
                "Праздник первых шагов ребёнка и символическое разрезание пут.",
                "Пожелание ребёнку лёгкого и уверенного жизненного пути.",
                TEST_SOURCES, IMG_PEOPLE);
        tushoo.addStep(1, "Подготовка к празднику", "Собираются родные и близкие.");
        tushoo.addStep(2, "Разрезание пут", "Символически разрезают путы, чтобы путь ребёнка был лёгким.");
        tushoo.addStep(3, "Угощение и благопожелания", "Гостей угощают, ребёнку желают здоровья.");

        Rite kyz = newRite("Свадебные", "Кыз узатуу",
                "Торжественные проводы невесты из родительского дома с благословениями.",
                "Торжественные проводы невесты из родительского дома с благословениями.",
                "Переход девушки в новую семью; благословение родителей и родственников.",
                TEST_SOURCES, IMG_HERO);
        kyz.addStep(1, "Подготовка к проводам", "Приготовления в доме родителей невесты.");
        kyz.addStep(2, "Проводы невесты", "Невесту провожают из родительского дома.");
        kyz.addStep(3, "Бата", "Старшие произносят благословение.");

        Rite bata = newRite("Бытовые", "Бата берүү",
                "Устное благословение старших, произносимое перед важным начинанием.",
                "Устное благословение старших, произносимое перед важным начинанием.",
                "Уважение к старшим; пожелание удачи и благополучия.",
                TEST_SOURCES, IMG_VALLEY);
        bata.addStep(1, "Обращение к старшему", "Просят благословения у старшего.");
        bata.addStep(2, "Благословение", "Старший произносит бата.");
        bata.addStep(3, "Омин", "Проводят ладонями по лицу.");

        Rite as = newRite("Похоронные и поминальные", "Ас берүү",
                "Поминальный обряд в память об умершем.",
                "Поминальный обряд в память об умершем.",
                "Дань уважения умершему и поддержка семьи со стороны общины.",
                TEST_SOURCES, null);
        as.addStep(1, "Подготовка", "Готовятся к приёму гостей.");
        as.addStep(2, "Приём гостей", "Гостей угощают.");
        as.addStep(3, "Молитва", "Читают молитву и произносят благопожелания.");

        Rite nooruz = newRite("Праздничные", "Нооруз",
                "Праздник весеннего равноденствия и встречи нового года по традиционному календарю.",
                "Праздник весеннего равноденствия и встречи нового года по традиционному календарю.",
                "Обновление природы, достаток, мир и согласие.",
                TEST_SOURCES, null);
        nooruz.addStep(1, "Подготовка", "Уборка дома и подготовка угощения.");
        nooruz.addStep(2, "Праздничные блюда", "Готовят традиционные блюда.");
        nooruz.addStep(3, "Встреча гостей", "Праздничные гулянья.");

        riteRepository.saveAll(List.of(beshik, tushoo, kyz, bata, as, nooruz));
    }

    private Rite newRite(String categoryName, String title, String summary, String description,
                         String symbolism, String sources, String imageUrl) {
        Rite rite = new Rite();
        rite.setRiteCategory(categoryRepository.findByCategoryName(categoryName).orElseThrow());
        rite.setTitle(title);
        rite.setSummary(summary);
        rite.setDescription(description);
        rite.setSymbolism(symbolism);
        rite.setSources(sources);
        rite.setStatus(Rite.STATUS_PUBLISHED);
        if (imageUrl != null) {
            rite.getMedia().add(mediaFileRepository.save(new MediaFile(imageUrl, "IMAGE", title)));
        }
        return rite;
    }
}
