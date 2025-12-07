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

    private final String apiUrl;

    public CambioService(@Value("${api.url}") String apiUrl) {
        this.apiUrl = apiUrl;
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
        long CACHE_DURATION_MINUTES = 15;

        if (!cacheCotacao.containsKey("ultima_atualizacao")) {
            return false;
        }

        Instant lastUpdate = Instant.parse((String) cacheCotacao.get("ultima_atualizacao"));
        long elapsedMinutes = java.time.Duration.between(lastUpdate, Instant.now()).toMinutes();

        return elapsedMinutes < CACHE_DURATION_MINUTES;
    }

    private Map<String, Object> fetchNovaCotacao() {
        try {
            Map<String, Map<String, String>> apiResponse = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Map<String, String>>>() {});

            if (apiResponse != null && apiResponse.containsKey("USDBRL")) {
                Map<String, String> dadosMoeda = apiResponse.get("USDBRL");

                Double cotacaoBRL = Double.parseDouble(dadosMoeda.get("bid"));

                Map<String, Object> resultado = new HashMap<>();
                resultado.put("cotacao", cotacaoBRL);
                resultado.put("ultima_atualizacao", Instant.now().toString());

                cacheCotacao = resultado;
                System.out.println("Cotação atualizada via AwesomeAPI: " + cotacaoBRL);
                return resultado;
            }

        } catch (Exception e) {
            System.err.println("Erro ao chamar AwesomeAPI: " + e.getMessage());
        }

        if (cacheCotacao.containsKey("cotacao")) {
            return cacheCotacao;
        }
        return Map.of("cotacao", 5.50, "ultima_atualizacao", Instant.now().toString());
    }

    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void atualizarCotacaoAutomatica() {
        System.out.println("Executando atualização agendada (15 min)...");
        fetchNovaCotacao();
    }
}