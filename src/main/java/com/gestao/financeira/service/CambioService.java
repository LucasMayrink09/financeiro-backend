package com.gestao.financeira.service;

import org.springframework.beans.factory.annotation.Value; // <-- IMPORTE ESTE
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class CambioService {

    private final String apiKey;
    private final String apiBaseUrl;

    public CambioService(
            @Value("${exchange.api.key}") String apiKey,
            @Value("${exchange.api.url}") String apiBaseUrl
    ) {
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
    }

    private Map<String, Object> cacheCotacao = new HashMap<>();

    private final RestClient restClient = RestClient.create();

    public Map<String, Object> buscarCotacaoAtualizada() {
        if (cacheCotacao.containsKey("cotacao") && isCacheValid()) {
            return cacheCotacao;
        }

        return fetchNovaCotacao();
    }

    private boolean isCacheValid() {
        long CACHE_DURATION_MINUTES = 60;

        if (!cacheCotacao.containsKey("ultima_atualizacao")) {
            return false;
        }

        Instant lastUpdate = Instant.parse((String) cacheCotacao.get("ultima_atualizacao"));
        long elapsedMinutes = java.time.Duration.between(lastUpdate, Instant.now()).toMinutes();

        return elapsedMinutes < CACHE_DURATION_MINUTES;
    }

    private Map<String, Object> fetchNovaCotacao() {
        String url = apiBaseUrl + apiKey + "/latest/USD";

        try {
            Map<String, Object> apiResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> conversionRates = (Map<String, Object>) apiResponse.get("conversion_rates");
            Double cotacaoBRL = (Double) conversionRates.get("BRL");

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("cotacao", cotacaoBRL);
            resultado.put("ultima_atualizacao", Instant.now().toString());

            cacheCotacao = resultado;
            return resultado;

        } catch (Exception e) {
            System.err.println("Erro ao chamar ExchangeRate-API: " + e.getMessage());

            if (cacheCotacao.containsKey("cotacao")) {
                return cacheCotacao;
            }

            return Map.of("cotacao", 5.50, "ultima_atualizacao", Instant.now().toString());
        }
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void atualizarCotacaoAutomatica() {
        System.out.println("Atualizando cotação automaticamente às " + Instant.now());
        fetchNovaCotacao();
    }
}