package com.gestao.financeira.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import sibApi.TransactionalEmailsApi;

import java.util.List;

@Service
@Slf4j
public class EmailService {

    private final TransactionalEmailsApi emailsApi;
    private final String fromName;
    private final String fromEmail;

    public EmailService(
            @Value("${brevo.api.key}") String apiKey,
            @Value("${app.email.from.name}") String fromName,
            @Value("${app.email.from.address}") String fromEmail
    ) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKeyAuth.setApiKey(apiKey);

        this.emailsApi = new TransactionalEmailsApi(defaultClient);
        this.fromName = fromName;
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String html) {
        try {
            SendSmtpEmail email = new SendSmtpEmail();
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(fromEmail);
            sender.setName(fromName);
            email.setSender(sender);

            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(to);
            email.setTo(List.of(recipient));
            email.setSubject(subject);
            email.setHtmlContent(html);
            emailsApi.sendTransacEmail(email);

            log.info("Email enviado com sucesso via Brevo para: {}", to);

        } catch (Exception e) {
            log.error("ERRO ao enviar email via Brevo para {}: {}", to, e.getMessage(), e);
        }
    }
}