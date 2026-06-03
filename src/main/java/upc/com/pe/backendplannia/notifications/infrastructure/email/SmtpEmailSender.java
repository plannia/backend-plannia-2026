package upc.com.pe.backendplannia.notifications.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.notifications.domain.services.EmailSender;

/**
 * Implementación de PRODUCCIÓN: envía correo real vía SMTP usando JavaMailSender
 * (autoconfigurado por spring-boot-starter-mail a partir de las propiedades spring.mail.*).
 * Se activa con notifications.email.transport=smtp.
 */
@Component
@ConditionalOnProperty(name = "notifications.email.transport", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${notifications.email.from:no-reply@plannia.com}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
