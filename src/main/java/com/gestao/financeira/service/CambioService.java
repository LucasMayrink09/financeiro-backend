package com.gestao.financeira.service;

import org.springframework.beans.factory.annotation.Value;
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
            @Value("${api.key}") String apiKey,
            @Value("${api.url}") String apiBaseUrl
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
        long CACHE_DURATION_MINUTES = 35;

        if (!cacheCotacao.containsKey("ultima_atualizacao")) {
            return false;
        }

        Instant lastUpdate = Instant.parse((String) cacheCotacao.get("ultima_atualizacao"));
        long elapsedMinutes = java.time.Duration.between(lastUpdate, Instant.now()).toMinutes();

        return elapsedMinutes < CACHE_DURATION_MINUTES;
    }

    private Map<String, Object> fetchNovaCotacao() {
        try {
            // CORREÇÃO AQUI: Montar a URL completa juntando a Base + "key=" + Chave
            // Sua base termina com "&", então adicionamos "key=" e a chave
            String urlCompleta = apiBaseUrl + "key=" + apiKey;

            Map<String, Object> apiResponse = restClient.get()
                    .uri(urlCompleta) // Usa a URL montada
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (apiResponse != null && apiResponse.containsKey("results")) {
                Map<String, Object> results = (Map<String, Object>) apiResponse.get("results");
                Map<String, Object> currencies = (Map<String, Object>) results.get("currencies");
                Map<String, Object> usdData = (Map<String, Object>) currencies.get("USD");

                Double cotacaoBRL = (Double) usdData.get("buy");

                Map<String, Object> resultado = new HashMap<>();
                resultado.put("cotacao", cotacaoBRL);
                resultado.put("ultima_atualizacao", Instant.now().toString());

                cacheCotacao = resultado;
                System.out.println("Cotação atualizada via HG Brasil: " + cotacaoBRL);
                return resultado;
            }

        } catch (Exception e) {
            System.err.println("Erro ao chamar HG Brasil: " + e.getMessage());
        }

        if (cacheCotacao.containsKey("cotacao")) {
            return cacheCotacao;
        }
        return Map.of("cotacao", 5.50, "ultima_atualizacao", Instant.now().toString());
    }

    @Scheduled(fixedRate = 35 * 60 * 1000)
    public void atualizarCotacaoAutomatica() {
        System.out.println("Executando atualização agendada (35 min)...");
        fetchNovaCotacao();
    }
}