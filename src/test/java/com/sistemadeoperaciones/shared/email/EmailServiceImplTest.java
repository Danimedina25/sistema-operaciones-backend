package com.sistemadeoperaciones.shared.email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceImplTest {

    @Test
    void sendsFromTheConfiguredVerifiedDomain() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender, "notificaciones@mail.se-estatus.com");

        service.sendEmail("persona@example.com", "Recuperación de contraseña", "Enlace de prueba");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("notificaciones@mail.se-estatus.com", message.getValue().getFrom());
        assertEquals("persona@example.com", message.getValue().getTo()[0]);
        assertEquals("Recuperación de contraseña", message.getValue().getSubject());
    }
}
