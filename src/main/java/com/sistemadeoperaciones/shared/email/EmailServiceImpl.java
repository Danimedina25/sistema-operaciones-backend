package com.sistemadeoperaciones.shared.email;

import com.sistemadeoperaciones.usuarios.exceptions.EmailSendException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

        } catch (MailAuthenticationException e) {
            throw new EmailSendException(
                    "No se pudo enviar el correo porque la configuración del servicio de email es inválida"
            );
        } catch (MailException e) {
            throw new EmailSendException(
                    "No se pudo enviar el correo de activación en este momento"
            );
        } catch (Exception e) {
            throw new EmailSendException(
                    "Ocurrió un problema al enviar el correo"
            );
        }
    }
}