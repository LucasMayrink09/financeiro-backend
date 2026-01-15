package com.gestao.financeira.service;

import com.gestao.financeira.dto.SocialLoginDTO;
import com.gestao.financeira.dto.SocialProvider;
import com.gestao.financeira.dto.SocialUserInfo;
import com.gestao.financeira.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.time.Instant;
import java.util.Map;

@Service
public class SocialService {

    @Value("${app.social.google-client-id}")
    private String googleClientId;
    @Value("${app.social.microsoft-client-id}")
    private String microsoftClientId;

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

    private SocialUserInfo validateGoogle(String token) {
        try {
            var response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("email") == null) {
                throw new RegraDeNegocioException("Token do Google inválido ou expirado.");
            }

            String aud = (String) response.get("aud");
            if (!googleClientId.equals(aud)) {
                throw new RegraDeNegocioException("Token não pertence a esta aplicação (Audience mismatch).");
            }

            String iss = (String) response.get("iss");
            if (!"accounts.google.com".equals(iss) &&
                    !"https://accounts.google.com".equals(iss)) {
                throw new RegraDeNegocioException("Issuer inválido do Google.");
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


    private SocialUserInfo validateMicrosoft(String accessToken) {
        try {
            // Microsoft Graph usa o ACCESS_TOKEN
            var response = restClient.get()
                    .uri("https://graph.microsoft.com/v1.0/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RegraDeNegocioException("Token da Microsoft inválido.");
            }

            String email = (String) response.get("mail");
            if (email == null) {
                email = (String) response.get("userPrincipalName");
            }

            if (email == null) {
                throw new RegraDeNegocioException("Não foi possível obter o email da conta Microsoft.");
            }

            Map<String, Object> claims = decodeJwtPayload(accessToken);

            String aud = (String) claims.get("aud");
            if (!microsoftClientId.equals(aud)) {
                throw new RegraDeNegocioException("Token Microsoft não pertence a esta aplicação");
            }

            String iss = (String) claims.get("iss");
            if (iss == null || !iss.contains("login.microsoftonline.com")) {
                throw new RegraDeNegocioException("Issuer inválido do Microsoft");
            }

            Long exp = ((Number) claims.get("exp")).longValue();
            long now = Instant.now().getEpochSecond();

            if (now > exp) {
                throw new RegraDeNegocioException("Token Microsoft expirado.");
            }


            String name = (String) response.get("displayName");

            return new SocialUserInfo(email, name, SocialProvider.MICROSOFT);
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro na validação Microsoft: " + e.getMessage());
        }
    }

    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("JWT inválido");
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1])
            );

            return new ObjectMapper().readValue(payloadJson, Map.class);
        } catch (Exception e) {
            throw new RegraDeNegocioException("Token JWT inválido");
        }
    }
}