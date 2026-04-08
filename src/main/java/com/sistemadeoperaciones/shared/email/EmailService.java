package com.sistemadeoperaciones.shared.email;

public interface EmailService {

    void sendEmail(String to, String subject, String text);
}