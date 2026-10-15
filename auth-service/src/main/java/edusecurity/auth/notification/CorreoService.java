package edusecurity.auth.notification;

import edusecurity.auth.config.AccesoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Envío de correos del sistema por SMTP. Si no hay servidor configurado
 * ({@code spring.mail.host} vacío) no se envía nada y se registra una advertencia,
 * sin escribir el enlace en el log.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AccesoProperties properties;
    private final String smtpHost;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender, AccesoProperties properties,
                         @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.smtpHost = smtpHost;
    }

    public void enviarRecuperacion(String destinatario, String nombre, String enlace) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(smtpHost)) {
            log.warn("SMTP no configurado (MAIL_HOST): no se envió el correo de recuperación");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(properties.getCorreoRemitente());
        mensaje.setTo(destinatario);
        mensaje.setSubject("EduSecurity: restablece tu contraseña");
        mensaje.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer la contraseña de tu cuenta de EduSecurity.
                Abre este enlace para crear una nueva (vence en %d minutos):

                %s

                Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.
                """.formatted(nombre, properties.getVigenciaRecuperacion().toMinutes(), enlace));

        try {
            sender.send(mensaje);
        } catch (MailException ex) {
            // No se propaga: la respuesta al usuario no debe revelar si el correo existe.
            log.error("No se pudo enviar el correo de recuperación: {}", ex.getMessage());
        }
    }
}
