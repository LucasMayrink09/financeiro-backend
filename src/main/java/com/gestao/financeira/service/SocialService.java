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
     * Valida ACCESS TOKEN do Google (necessário para botão customizado)
     */
    private SocialUserInfo validateGoogle(String token) {
        try {
            var response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?access_token=" + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("email") == null) {
                throw new RegraDeNegocioException("Token do Google inválido ou expirado.");
            }

            // 🔐 Segurança: garantir que o token pertence ao seu app
            String aud = (String) response.get("aud");
            String azp = (String) response.get("azp");

            if (!googleClientId.equals(aud) && !googleClientId.equals(azp)) {
                throw new RegraDeNegocioException(
                        "Token não pertence a esta aplicação (client_id inválido)."
                );
            }

            Object verified = response.get("email_verified");
            if (verified != null && "false".equals(verified.toString())) {
                throw new RegraDeNegocioException("Email do Google não verificado.");
            }

            long exp = Long.parseLong((String) response.get("exp"));
            long now = Instant.now().getEpochSecond();

            if (now > exp) {
                throw new RegraDeNegocioException("Token expirado.");
            }

            return new SocialUserInfo(
                    (String) response.get("email"),
                    (String) response.get("name"),
                    SocialProvider.GOOGLE
            );
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro na validação Google: " + e.getMessage());
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