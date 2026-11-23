package kg.murasinteractive.config;

import kg.murasinteractive.quiz.model.Difficulty;
import kg.murasinteractive.quiz.model.QuestionType;
import kg.murasinteractive.quiz.model.Quiz;
import kg.murasinteractive.quiz.model.QuizQuestion;
import kg.murasinteractive.quiz.model.QuizTopic;
import kg.murasinteractive.quiz.repository.QuizRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Заполняет пустую базу тремя викторинами по темам разделов (юрта, обряды, герои и фольклор).
 * В каждой есть вопросы всех четырёх типов. Вопросы построены на материалах сайта:
 * если измените материал, проверьте и вопрос.
 */
@Component
public class QuizSeeder implements CommandLineRunner {

    private final QuizRepository quizRepository;

    public QuizSeeder(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (quizRepository.count() > 0) {
            return;
        }
        quizRepository.saveAll(List.of(yurtQuiz(), ritesQuiz(), folkloreQuiz()));
    }

    private Quiz yurtQuiz() {
        Quiz quiz = new Quiz("Устройство юрты", QuizTopic.YURT, Difficulty.EASY,
                "Проверьте, как хорошо вы знаете элементы кыргызской юрты.");

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "Как называется круглый деревянный обод в вершине купола юрты?", 10,
                        "Түндүк замыкает и держит купол, а ещё служит световым окном и дымоходом.")
                .addOption("Кереге", false)
                .addOption("Түндүк", true)
                .addOption("Уук", false)
                .addOption("Тизгич", false);

        quiz.addQuestion(QuestionType.MULTIPLE_CHOICE,
                        "Какие функции выполняет түндүк? Выберите все верные.", 10,
                        "Түндүк — световое окно и дымоход для очага. Стены юрты держит кереге, а вход закрывает дверь.")
                .addOption("Служит световым окном", true)
                .addOption("Служит дымоходом для очага", true)
                .addOption("Держит войлочные стены", false)
                .addOption("Закрывает вход в юрту", false);

        quiz.addQuestion(QuestionType.TRUE_FALSE,
                        "Перекрещённые дуги чамгарака изображены внутри солнца на государственном флаге Кыргызстана.", 10,
                        "Да: чамгарак — перекрестие дуг внутри обода түндүка, оно изображено на флаге.")
                .addTrueFalse(true);

        quiz.addQuestion(QuestionType.MATCHING,
                        "Сопоставьте элемент юрты и его роль.", 15,
                        "Түндүк замыкает купол, уук образует каркас крыши, кереге — решётчатая стена, тизгич стягивает ууки.")
                .addPair("Түндүк", "Замыкает купол и служит световым окном")
                .addPair("Уук", "Жердь купола, передаёт вес крыши на стены")
                .addPair("Кереге", "Решётчатая складная стена")
                .addPair("Тизгич", "Тесьма, стягивающая ууки");

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "К чему привязываются нижние концы ууков?", 10,
                        "Верхние концы ууков входят в түндүк, а нижние привязываются к верхушкам кереге.")
                .addOption("К түндүку", false)
                .addOption("К чамгараку", false)
                .addOption("К верхушкам кереге", true)
                .addOption("К очагу", false);
        return quiz;
    }

    private Quiz ritesQuiz() {
        Quiz quiz = new Quiz("Обряды и традиции", QuizTopic.RITES, Difficulty.EASY,
                "Проверьте, что вы знаете об обрядах кыргызского народа.");

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "Как называется традиционная кыргызская колыбель?", 10,
                        "Колыбель называется бешик; ей посвящён обряд бешик той.")
                .addOption("Тундук", false)
                .addOption("Шырдак", false)
                .addOption("Бешик", true)
                .addOption("Тебетей", false);

        quiz.addQuestion(QuestionType.TRUE_FALSE,
                        "Бешик той — праздник по случаю первого укладывания ребёнка в колыбель.", 10,
                        "Да, обряд посвящён первому укладыванию младенца в бешик.")
                .addTrueFalse(true);

        quiz.addQuestion(QuestionType.MULTIPLE_CHOICE,
                        "Какие обряды связаны с рождением и ростом ребёнка? Выберите все верные.", 10,
                        "Бешик той и тушоо кесүү посвящены ребёнку; кыз узатуу — свадебный обряд, ас берүү — поминальный.")
                .addOption("Бешик той", true)
                .addOption("Тушоо кесүү", true)
                .addOption("Кыз узатуу", false)
                .addOption("Ас берүү", false);

        quiz.addQuestion(QuestionType.MATCHING,
                        "Сопоставьте обряд и его смысл.", 15,
                        "Каждый обряд отмечает свой этап жизни: рождение, первые шаги, свадьбу и память об ушедших.")
                .addPair("Бешик той", "Первое укладывание младенца в колыбель")
                .addPair("Тушоо кесүү", "Праздник первых шагов ребёнка")
                .addPair("Кыз узатуу", "Проводы невесты из родительского дома")
                .addPair("Ас берүү", "Поминальный обряд");

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "Что означает слово «бата»?", 10,
                        "Бата — устное благословение старших, которое произносят перед важным начинанием.")
                .addOption("Благословение старших", true)
                .addOption("Свадебный наряд", false)
                .addOption("Поминальное угощение", false)
                .addOption("Колыбельная песня", false);
        return quiz;
    }

    private Quiz folkloreQuiz() {
        Quiz quiz = new Quiz("Герои и фольклор", QuizTopic.FOLKLORE, Difficulty.MEDIUM,
                "Эпос, сказки и легенды кыргызского народа.");

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "Кто такой Семетей?", 10,
                        "Семетей — сын Манаса, герой второй части эпической трилогии.")
                .addOption("Мудрый советник Манаса", false)
                .addOption("Сын Манаса", true)
                .addOption("Хитрец из народных сказок", false)
                .addOption("Жена Манаса", false);

        quiz.addQuestion(QuestionType.MULTIPLE_CHOICE,
                        "Какие произведения входят в эпическую трилогию? Выберите все верные.", 10,
                        "Трилогия состоит из эпосов «Манас», «Семетей» и «Сейтек». «Кырк кыз» — легенда, Алдар Көсө — герой сказок.")
                .addOption("Манас", true)
                .addOption("Семетей", true)
                .addOption("Сейтек", true)
                .addOption("Кырк кыз", false);

        quiz.addQuestion(QuestionType.MATCHING,
                        "Сопоставьте героя и его описание.", 15,
                        "Манас — объединитель племён, Бакай — советник, Каныкей — мудрая жена Манаса, Алдар Көсө — хитрец из сказок.")
                .addPair("Манас", "Богатырь, объединивший кыргызские племена")
                .addPair("Бакай", "Мудрый советник Манаса")
                .addPair("Каныкей", "Жена Манаса, мудрая героиня эпоса")
                .addPair("Алдар Көсө", "Хитрец, герой народных сказок");

        quiz.addQuestion(QuestionType.TRUE_FALSE,
                        "Сейтек — сын Манаса.", 10,
                        "Нет: Сейтек — сын Семетея, то есть внук Манаса.")
                .addTrueFalse(false);

        quiz.addQuestion(QuestionType.SINGLE_CHOICE,
                        "С каким преданием связывают происхождение названия «кыргыз»?", 10,
                        "Название «кыргыз» связывают с преданием о сорока девушках — «кырк кыз».")
                .addOption("Сказки об Алдар Көсө", false)
                .addOption("Кырк кыз — предание о сорока девушках", true)
                .addOption("Эпос «Семетей»", false)
                .addOption("Обряд бешик той", false);
        return quiz;
    }
}
