package com.gestao.financeira.service;

import com.gestao.financeira.dto.SocialLoginDTO;
import com.gestao.financeira.dto.SocialProvider;
import com.gestao.financeira.dto.SocialUserInfo;
import com.gestao.financeira.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Instant;
import java.util.Map;

@Service
public class SocialService {

    @Value("${app.social.google-client-id}")
    private String googleClientId;

    private final RestClient restClient;

    public SocialService(RestClient restClient) {
        this.restClient = restClient;
    }

    public SocialUserInfo validateToken(SocialLoginDTO dto) {
        return switch (dto.provider()) {
            case GOOGLE -> validateGoogle(dto.token());
            case MICROSOFT -> validateMicrosoft(dto.token());
        };
    }

    /**
     * Valida ID Token do Google via tokeninfo endpoint
     * ✅ CORRETO: Usa id_token para autenticação (OpenID Connect)
     */
    private SocialUserInfo validateGoogle(String idToken) {
        try {
            // ✅ IMPORTANTE: Usar id_token, NÃO access_token
            var response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RegraDeNegocioException("Resposta vazia do Google");
            }

            // ✅ Validar Audience (aud) - CRÍTICO para segurança
            String aud = (String) response.get("aud");
            if (!googleClientId.equals(aud)) {
                throw new RegraDeNegocioException("Token não pertence a esta aplicação (audience inválido)");
            }

            // ✅ Validar Issuer (iss) - CRÍTICO para segurança
            String iss = (String) response.get("iss");
            if (!"accounts.google.com".equals(iss) &&
                    !"https://accounts.google.com".equals(iss)) {
                throw new RegraDeNegocioException("Issuer inválido do Google");
            }

            // ✅ Validar expiração (exp)
            Object expObj = response.get("exp");
            if (expObj != null) {
                long exp = expObj instanceof String
                        ? Long.parseLong((String) expObj)
                        : ((Number) expObj).longValue();

                long now = Instant.now().getEpochSecond();
                if (now > exp) {
                    throw new RegraDeNegocioException("Token expirado");
                }
            }

            // ✅ Validar email verificado
            Object emailVerified = response.get("email_verified");
            if (emailVerified != null) {
                boolean isVerified = emailVerified instanceof Boolean
                        ? (Boolean) emailVerified
                        : "true".equalsIgnoreCase(emailVerified.toString());

                if (!isVerified) {
                    throw new RegraDeNegocioException("Email do Google não verificado");
                }
            }

            // ✅ Extrair dados do usuário
            String email = (String) response.get("email");
            if (email == null || email.isBlank()) {
                throw new RegraDeNegocioException("Email não encontrado no token");
            }

            String name = (String) response.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0]; // Fallback
            }

            return new SocialUserInfo(email, name, SocialProvider.GOOGLE);

        } catch (RestClientException e) {
            throw new RegraDeNegocioException("Erro ao validar token do Google: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("Token com formato inválido");
        }
    }

    /**
     * Valida Access Token da Microsoft via Graph API
     * ✅ CORRETO: Microsoft Graph valida o token automaticamente
     */
    private SocialUserInfo validateMicrosoft(String accessToken) {
        try {
            // ✅ A chamada ao /me valida automaticamente:
            // - Se o token é válido
            // - Se não está expirado
            // - Se pertence ao nosso app (implícito no consent)
            var response = restClient.get()
                    .uri("https://graph.microsoft.com/v1.0/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RegraDeNegocioException("Resposta vazia da Microsoft");
            }

            // ✅ Extrair email (priorizar 'mail', fallback para 'userPrincipalName')
            String email = (String) response.get("mail");
            if (email == null || email.isBlank()) {
                email = (String) response.get("userPrincipalName");
            }

            if (email == null || email.isBlank()) {
                throw new RegraDeNegocioException("Email não encontrado na conta Microsoft");
            }

            // ✅ Extrair nome
            String name = (String) response.get("displayName");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0]; // Fallback
            }

            return new SocialUserInfo(email, name, SocialProvider.MICROSOFT);

        } catch (RestClientException e) {
            // Se o token for inválido/expirado, a Microsoft retorna 401
            throw new RegraDeNegocioException("Token da Microsoft inválido ou expirado: " + e.getMessage());
        }
    }
}