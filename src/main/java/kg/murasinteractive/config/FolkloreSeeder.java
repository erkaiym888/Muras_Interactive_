package kg.murasinteractive.config;

import kg.murasinteractive.folklore.model.FolkloreHero;
import kg.murasinteractive.folklore.model.FolkloreType;
import kg.murasinteractive.folklore.model.FolkloreWork;
import kg.murasinteractive.folklore.repository.FolkloreHeroRepository;
import kg.murasinteractive.folklore.repository.FolkloreTypeRepository;
import kg.murasinteractive.folklore.repository.FolkloreWorkRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Заполняет пустую базу справочником типов и несколькими героями и произведениями.
 * Тексты - заготовки: перед защитой сверьте их с настоящими источниками
 * и добавьте иллюстрации (сейчас у карточек картинок нет).
 */
@Component
public class FolkloreSeeder implements CommandLineRunner {

    private final FolkloreTypeRepository typeRepository;
    private final FolkloreHeroRepository heroRepository;
    private final FolkloreWorkRepository workRepository;

    public FolkloreSeeder(FolkloreTypeRepository typeRepository, FolkloreHeroRepository heroRepository,
                          FolkloreWorkRepository workRepository) {
        this.typeRepository = typeRepository;
        this.heroRepository = heroRepository;
        this.workRepository = workRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (typeRepository.count() == 0) {
            for (String name : List.of("Герои", "Эпос", "Сказки", "Легенды", "Пословицы и поговорки")) {
                typeRepository.save(new FolkloreType(name));
            }
        }
        if (heroRepository.count() > 0 || workRepository.count() > 0) {
            return;
        }

        FolkloreWork manas = work("Манас", "Эпос",
                "Героический эпос кыргызского народа о богатыре Манасе и его соратниках — одно из самых "
                        + "объёмных произведений устного народного творчества.");
        FolkloreWork semetei = work("Семетей", "Эпос",
                "Вторая часть эпической трилогии о Манасе и его потомках: рассказ о сыне Манаса.");
        FolkloreWork seitek = work("Сейтек", "Эпос",
                "Третья часть эпической трилогии: рассказ о Сейтеке, внуке Манаса.");
        FolkloreWork aldar = work("Сказки об Алдар Көсө", "Сказки",
                "Цикл народных сказок о находчивом хитреце, который остроумно побеждает жадных и "
                        + "несправедливых людей.");
        FolkloreWork kyrkKyz = work("Кырк кыз", "Легенды",
                "Предание о сорока девушках, с которым связывают происхождение названия «кыргыз».");
        FolkloreWork proverbs = work("Пословицы и поговорки кыргызов", "Пословицы и поговорки",
                "Короткие образные изречения, в которых народ передал жизненный опыт, мудрость и "
                        + "представления о добре и чести.");
        workRepository.saveAll(List.of(manas, semetei, seitek, aldar, kyrkKyz, proverbs));

        heroRepository.saveAll(List.of(
                hero("Манас", "Герои",
                        "Главный герой эпоса «Манас» — богатырь, объединивший кыргызские племена.", manas),
                hero("Семетей", "Герои",
                        "Сын Манаса, герой второй части эпической трилогии.", semetei),
                hero("Сейтек", "Герои",
                        "Сын Семетея, герой третьей части эпической трилогии.", seitek),
                hero("Каныкей", "Герои",
                        "Жена Манаса, мудрая и дальновидная героиня эпоса.", manas, semetei),
                hero("Бакай", "Герои",
                        "Мудрый старец, советник и наставник Манаса.", manas),
                hero("Алдар Көсө", "Сказки",
                        "Народный герой-хитрец, персонаж многих кыргызских сказок.", aldar)));
    }

    private FolkloreWork work(String title, String typeName, String summary) {
        FolkloreWork work = new FolkloreWork();
        work.setTitle(title);
        work.setWorkType(typeRepository.findByTypeName(typeName).orElseThrow());
        work.setSummary(summary);
        return work;
    }

    private FolkloreHero hero(String name, String typeName, String description, FolkloreWork... works) {
        FolkloreHero hero = new FolkloreHero();
        hero.setName(name);
        hero.setHeroType(typeRepository.findByTypeName(typeName).orElseThrow());
        hero.setDescription(description);
        hero.getWorks().addAll(List.of(works));
        return hero;
    }
}
