package upc.com.pe.backendplannia.notifications.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.notifications.domain.services.EmailSender;

/**
 * Implementación de DESARROLLO: no envía correo real, solo lo escribe en el log.
 * Es el bean por defecto (se activa salvo que notifications.email.transport=smtp).
 */
@Component
@ConditionalOnProperty(name = "notifications.email.transport", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL-LOG] to='{}' | subject='{}' | body='{}'", to, subject, body);
    }
}
