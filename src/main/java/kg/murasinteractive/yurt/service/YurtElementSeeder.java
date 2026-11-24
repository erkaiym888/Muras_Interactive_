package kg.murasinteractive.yurt.service;

import kg.murasinteractive.yurt.model.YurtElement;
import kg.murasinteractive.yurt.repository.YurtElementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Элементы юрты в базе (таблица yurt_element из п. 4.4 ТЗ) - чтобы находить их поиском (FR-4.1)
 * и связывать с обрядами (FR-3.3). Ключи совпадают с ключами схемы в design/src/yurt.ts.
 * Добавляются только отсутствующие элементы, правки в базе не перезаписываются.
 */
@Component
@Order(5)
public class YurtElementSeeder implements CommandLineRunner {

    private record Seed(String key, String name, String group, String description) {}

    private static final List<Seed> ELEMENTS = List.of(
            new Seed("chamgarak", "Чамгарак", "Каркас",
                    "Изогнутые деревянные дуги, которые перекрещиваются внутри обода түндүка и образуют его выпуклый купол."),
            new Seed("tunduk", "Түндүк", "Каркас",
                    "Круглый деревянный обод в вершине купола. В его отверстия вставляются верхние концы всех ууков."),
            new Seed("uuk", "Уук", "Каркас",
                    "Длинные изогнутые жерди купола. Верхние концы входят в түндүк, нижние привязываются к верхушкам кереге."),
            new Seed("tizgich", "Тизгич", "Связки",
                    "Узкая тесьма, которая проходит по кругу через все ууки примерно на середине купола."),
            new Seed("kerege", "Кереге", "Каркас",
                    "Сборно-раздвижная решётчатая стена. Состоит из отдельных секций — канатов, связанных между собой ремнями."),
            new Seed("artkyUzuk", "Арткы узук", "Покрытие",
                    "Войлочное полотнище, которое накрывает верхнюю, заднюю часть купола над ууками."),
            new Seed("astynkyUzuk", "Астынкы узук", "Покрытие",
                    "Нижнее войлочное полотнище купола, которое ложится на ууки от кереге до верхнего узука."),
            new Seed("tuurduk", "Туурдук", "Покрытие",
                    "Длинные войлочные полотнища, которыми обёртывают кереге снаружи, поверх чия."),
            new Seed("eshik", "Эшик", "Вход",
                    "Вход в юрту: деревянная дверная рама (босого) и войлочный полог, который днём сворачивают и подвязывают сверху."),
            new Seed("chiy", "Чий", "Убранство",
                    "Циновка из стеблей степной травы чий. Каждый стебель обматывают цветной шерстью, и из них складывается орнамент."),
            new Seed("kyrchoo", "Кырчоо", "Связки",
                    "Плетёные шерстяные верёвки, которые опоясывают юрту снаружи по стенам и куполу."),
            new Seed("uuktunMatoosu", "Ууктун матоосу", "Связки",
                    "Петли и ремешки, которыми нижние концы ууков привязывают к развилкам на верхушках кереге."),
            new Seed("tundukJabuu", "Түндүк жабуу", "Покрытие",
                    "Отдельный квадратный кусок войлока, накрывающий түндүк сверху. К его углам привязаны верёвки."),
            new Seed("keregeChalgych", "Кереге чалгыч", "Связки",
                    "Широкая тканая лента, которая опоясывает кереге по самому верху, там, где к стенам крепятся ууки."),
            new Seed("jabykBash", "Жабык баш", "Убранство",
                    "Полоса из войлока или ткани с вышитым орнаментом и кистями, которая идёт по кругу по уукам внутри юрты."));

    private final YurtElementRepository repository;

    public YurtElementSeeder(YurtElementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int order = 1;
        for (Seed s : ELEMENTS) {
            if (repository.findByElementKey(s.key()).isEmpty()) {
                repository.save(new YurtElement(s.key(), s.name(), s.group(), s.description(), order));
            }
            order++;
        }
    }
}
