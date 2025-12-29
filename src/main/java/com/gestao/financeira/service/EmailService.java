package com.gestao.financeira.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    public EmailService(@Value("${resend.api.key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String html) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .from("FinanceX <onboarding@resend.dev>")
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();

            SendEmailResponse response = resend.emails().send(request);

            log.info(
                    "Email enviado via Resend. ID: {} | Destinatário: {}",
                    response.getId(),
                    to
            );

        } catch (Exception e) {
            log.error(
                    "ERRO ao enviar email via Resend para {}: {}",
                    to,
                    e.getMessage(),
                    e
            );
        }
    }
}