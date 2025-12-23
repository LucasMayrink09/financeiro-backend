package com.gestao.financeira.externalservice;

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

    private Map<String, Object> fetchNovaCotacao() {
        try {
            String url = montarUrlApi();
            Map<String, Object> respostaApi = executarChamadaHttp(url);
            Double valorCotacao = extrairValorCotacao(respostaApi);

            if (valorCotacao != null) {
                return atualizarCache(valorCotacao);
            }

        } catch (Exception e) {
            System.err.println("Erro no fluxo de cotação: " + e.getMessage());
        }

        return obterFallback();
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

    private String montarUrlApi() {
        return apiBaseUrl + "key=" + apiKey;
    }

    private Map<String, Object> executarChamadaHttp(String url) {
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private Double extrairValorCotacao(Map<String, Object> apiResponse) {
        if (apiResponse != null && apiResponse.containsKey("results")) {
            Map<String, Object> results = (Map<String, Object>) apiResponse.get("results");
            Map<String, Object> currencies = (Map<String, Object>) results.get("currencies");
            Map<String, Object> usdData = (Map<String, Object>) currencies.get("USD");

            if (usdData != null && usdData.containsKey("buy")) {
                return (Double) usdData.get("buy");
            }
        }
        return null;
    }

    private Map<String, Object> atualizarCache (Double cotacaoValor) {
        Map<String, Object> novoResultado = new HashMap<>();
        novoResultado.put("cotacao", cotacaoValor);
        novoResultado.put("ultima_atualizacao", Instant.now().toString());
        this.cacheCotacao = novoResultado;
        return novoResultado;
    }

    private Map<String, Object> obterFallback () {
        if (cacheCotacao.containsKey("cotacao")) {
            return cacheCotacao;
        }
        return Map.of("cotacao", 5.50, "ultima_atualizacao", Instant.now().toString());
    }

    @Scheduled(fixedRate = 35 * 60 * 1000)
    public void updateQuoteAutomatically() {
        fetchNovaCotacao();
    }
}