package com.gestao.financeira.security;


import com.gestao.financeira.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .filter(origin -> !origin.equals("*"))
                .toArray(String[]::new);

        if (origins.length == 0) {
            throw new RegraDeNegocioException("ERRO FATAL: Nenhuma origem CORS válida configurada!");
        }

        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowCredentials(false)
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}