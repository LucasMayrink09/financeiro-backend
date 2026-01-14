package com.gestao.financeira.service;

import com.gestao.financeira.dto.GeminiRequestDTO;
import com.gestao.financeira.dto.GeminiResponseDTO;
import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiService {

    private final RestClient restClient;
    private final RateLimitService rateLimitService;
    private final SaldoService saldoService;

    @Value("${ai.gemini.url}")
    private String geminiUrl;

    @Value("${ai.gemini.key}")
    private String geminiKey;

    public AiService(RateLimitService rateLimitService, SaldoService saldoService) {
        this.rateLimitService = rateLimitService;
        this.saldoService = saldoService;
        this.restClient = RestClient.create();
    }

    @Cacheable(value = "ai-analise", key = "#user.email", unless = "#result == null")
    public String analisarInvestimentos(User user) {
        rateLimitService.consume("ai_analysis:" + user.getEmail(), 1, 1000);
        List<Saldo> saldos = saldoService.listarDoUsuario(user);

        if (saldos == null || saldos.isEmpty()) {
            return "Você ainda não possui investimentos cadastrados para análise. Adicione saldos e tente novamente.";
        }

        try {
            String prompt = construirPromptOtimizado(saldos);
            GeminiResponseDTO response = restClient.post()
                    .uri(geminiUrl + geminiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiRequestDTO.of(prompt))
                    .retrieve()
                    .body(GeminiResponseDTO.class);

            if (response == null || response.getText() == null) {
                throw new AiServiceException("A IA não retornou nenhuma sugestão válida.");
            }

            return response.getText();

        } catch (Exception e) {
            log.error("Erro API Gemini para usuário {}: {}", user.getEmail(), e.getMessage());
            throw new AiServiceException("O consultor financeiro está indisponível no momento. Tente mais tarde.");
        }
    }

    private String construirPromptOtimizado(List<Saldo> saldos) {
        String resumoSaldos = saldos.stream()
                .filter(s -> s.getValor() != null && s.getValor().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Saldo::getValor).reversed())
                .limit(30)
                .map(s -> String.format(
                        "- Conta: %s | Tipo: %s | Valor: %s %s",
                        s.getNomeConta(),
                        s.getTipo(), // Enum já vira string
                        s.getMoeda() != null ? s.getMoeda() : "BRL",
                        s.getValor()
                ))
                .collect(Collectors.joining("\n"));

        return """
                Você é um consultor financeiro sênior.
                
                Analise a carteira abaixo (os 30 maiores ativos) e gere exatamente 3 recomendações objetivas.
                Cada recomendação deve:
                - Ter no máximo 2 linhas
                - Ser prática e direta
                - Não usar introduções genéricas ("Olá", "Com base...")
                - Não repetir apenas o que já está na lista
                
                Formato da resposta:
                **1. Título da Dica 1:** Texto da dica.
                **2. Título da Dica 2:** Texto da dica.
                **3. Título da Dica 3:** Texto da dica.
                
                Carteira:
                %s
                """.formatted(resumoSaldos);
    }
}