package com.gestao.financeira.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = createHtmlMessage(to, subject, html);
            mailSender.send(message);
            log.debug("Email enviado com sucesso para: {}", to);
        } catch (MessagingException e) {
            log.error("ERRO CRÍTICO ao enviar email para {}: {}", to, e.getMessage(), e);
        }
    }

    private MimeMessage createHtmlMessage(String to, String subject, String html)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        return message;
    }
}