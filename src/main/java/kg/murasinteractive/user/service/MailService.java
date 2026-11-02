package kg.murasinteractive.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Отправка писем. Пока app.mail.enabled=false (по умолчанию), письма не отправляются,
 * а ссылка для восстановления пароля печатается в консоль - так удобно проверять без SMTP.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.enabled:false}") boolean enabled,
                       @Value("${app.mail.from:noreply@muras.local}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void sendPasswordReset(String to, String link) {
        if (!enabled) {
            log.warn("Почта выключена (app.mail.enabled=false). Ссылка для восстановления пароля для {}: {}", to, link);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.error("JavaMailSender не настроен, письмо для {} не отправлено", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Восстановление доступа — Muras Interactive");
        message.setText("Здравствуйте!\n\n"
                + "Вы запросили восстановление пароля на сайте Muras Interactive.\n"
                + "Чтобы задать новый пароль, перейдите по ссылке (она действует 1 час):\n\n"
                + link + "\n\n"
                + "Если вы ничего не запрашивали, просто проигнорируйте это письмо: пароль останется прежним.");
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            // Ошибку не пробрасываем наружу: иначе по ответу сервера можно узнать, есть ли такой адрес.
            log.error("Не удалось отправить письмо на {}: {}", to, e.getMessage());
        }
    }
}
