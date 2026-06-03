package upc.com.pe.backendplannia.notifications.domain.services;

// Puerto de salida hacia el "mundo del correo". El dominio no sabe si detrás hay un log, SMTP,
// SendGrid, etc. La implementación concreta se elige por configuración (ver infrastructure/email).
public interface EmailSender {
    void send(String to, String subject, String body);
}
