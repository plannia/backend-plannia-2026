package upc.com.pe.backendplannia.notifications.infrastructure.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Prueba de envío REAL por Resend (SMTP). Verifica las credenciales y el dominio verificado.
 *
 * NO corre en la suite normal: está protegida por @EnabledIfEnvironmentVariable. Solo se ejecuta
 * cuando la variable de entorno RESEND_LIVE_TEST=true (así no se gasta cuota en cada `mvn test`).
 *
 * Lee las credenciales del application.properties real (resuelve ${MAIL_*} y ${notifications.email.from}).
 */
@SpringJUnitConfig
@ContextConfiguration(classes = ResendEmailLiveTest.ResendMailTestConfiguration.class)
// application-dev.properties (gitignored, segundo → gana) aporta la API key real de Resend en local.
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-dev.properties"})
@EnabledIfEnvironmentVariable(named = "RESEND_LIVE_TEST", matches = "true")
class ResendEmailLiveTest {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailLiveTest.class);
    private static final String RECIPIENT = "u202210838@upc.edu.pe";

    @Autowired
    private SmtpEmailSender emailSender;

    @Test
    void sendsRealEmailViaResend() {
        log.info("Enviando correo de prueba a {} vía Resend...", RECIPIENT);
        emailSender.send(
                RECIPIENT,
                "Prueba Plannia ✉️ Resend",
                "Si lees esto, el envío de correo por Resend (SMTP) desde backend-plannia funciona. ✅");
        log.info("Correo enviado sin excepción. Revisa la bandeja de {} (y spam).", RECIPIENT);
    }

    @TestConfiguration
    static class ResendMailTestConfiguration {
        @Bean
        JavaMailSender javaMailSender(
                @Value("${spring.mail.host}") String host,
                @Value("${spring.mail.port}") int port,
                @Value("${spring.mail.username}") String username,
                @Value("${spring.mail.password}") String password
        ) {
            var sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            var props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            return sender;
        }

        @Bean
        SmtpEmailSender smtpEmailSender(
                JavaMailSender mailSender,
                @Value("${notifications.email.from}") String from
        ) {
            return new SmtpEmailSender(mailSender, from);
        }
    }
}
