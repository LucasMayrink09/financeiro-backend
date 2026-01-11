package com.gestao.financeira.externalservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RendaFixaService {

    private final String apiGovUrl;
    private final RestClient restClient = RestClient.create();

    // Guarda CDI, SELIC, IPCA
    private Map<String, Object> cacheIndices = new ConcurrentHashMap<>();

    private static final String CODIGO_CDI = "12";   // Taxa DI diária
    private static final String CODIGO_SELIC = "11"; // Taxa Selic diária
    private static final String CODIGO_IPCA = "433"; // IPCA Mensal

    public RendaFixaService(@Value("${api.gov-url}") String apiGovUrl) {
        this.apiGovUrl = apiGovUrl;
    }

    /**
     * Retorna um Map com os índices atuais. Ex: {"CDI": 0.0501, "IPCA": 0.54}
     */
    public Map<String, Object> buscarIndicesAtuais() {
        if (cacheIndices.containsKey("ultima_atualizacao") && isCacheValid()) {
            return cacheIndices;
        }
        return orquestrarBuscaIndices();
    }

    /**
     * Calcula quanto o saldo rendeu hoje baseado na taxa atual.
     * Fórmula: Saldo * (TaxaCDI / 100) * (PercentualInvestido / 100)
     */
    public BigDecimal calcularRendimentoDiario(BigDecimal saldoAtual, BigDecimal percentualOuTaxa, String nomeIndice) {
        if (saldoAtual == null || percentualOuTaxa == null) return BigDecimal.ZERO;

        String indiceAlvo = (nomeIndice == null || nomeIndice.isBlank()) ? "CDI" : nomeIndice.toUpperCase();

        if ("PRE".equals(indiceAlvo)) {
            BigDecimal taxaDiariaPre = percentualOuTaxa
                    .divide(BigDecimal.valueOf(252), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            return saldoAtual.multiply(taxaDiariaPre).setScale(2, RoundingMode.HALF_UP);
        }

        Double taxaIndice = obterTaxaDoCache(indiceAlvo);
        if (taxaIndice == null) return BigDecimal.ZERO;

        BigDecimal taxaDecimal;

        if ("IPCA".equals(indiceAlvo)) {
            taxaDecimal = BigDecimal.valueOf(taxaIndice)
                    .divide(BigDecimal.valueOf(22), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        } else {
            taxaDecimal = BigDecimal.valueOf(taxaIndice)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        }
        BigDecimal fatorContratado = percentualOuTaxa.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        return saldoAtual.multiply(taxaDecimal).multiply(fatorContratado).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> orquestrarBuscaIndices() {
        log.info("Atualizando índices de Renda Fixa (BCB)...");
        Map<String, Object> novosIndices = new ConcurrentHashMap<>();

        novosIndices.put("CDI", buscarIndiceNoGov(CODIGO_CDI, "CDI"));
        novosIndices.put("SELIC", buscarIndiceNoGov(CODIGO_SELIC, "SELIC"));
        novosIndices.put("IPCA", buscarIndiceNoGov(CODIGO_IPCA, "IPCA")); // Atenção: IPCA é mensal

        novosIndices.put("ultima_atualizacao", Instant.now().toString());
        this.cacheIndices = novosIndices;
        return novosIndices;
    }

    private Double buscarIndiceNoGov(String codigoSerie, String nomeIndice) {
        try {
            String url = apiGovUrl.replace("{codigo}", codigoSerie);

            List<Map<String, String>> resposta = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, String>>>() {});

            if (resposta != null && !resposta.isEmpty()) {
                String valorString = resposta.get(0).get("valor");
                return Double.parseDouble(valorString.replace(",", "."));
            }
        } catch (Exception e) {
            log.error("Erro ao buscar índice {}: {}", nomeIndice, e.getMessage());
        }
        return obterFallback(nomeIndice);
    }

    private Double obterTaxaDoCache(String chave) {
        buscarIndicesAtuais();
        return (Double) cacheIndices.get(chave);
    }

    private boolean isCacheValid() {
        // Cache de 12 horas
        long CACHE_DURATION_HOURS = 12;
        if (!cacheIndices.containsKey("ultima_atualizacao")) return false;
        Instant lastUpdate = Instant.parse((String) cacheIndices.get("ultima_atualizacao"));
        return java.time.Duration.between(lastUpdate, Instant.now()).toHours() < CACHE_DURATION_HOURS;
    }

    private Double obterFallback(String indice) {
        return switch (indice) {
            case "CDI", "SELIC" -> 0.045;
            case "IPCA" -> 0.40;
            default -> 0.0;
        };
    }

    // Atualiza automaticamente as 8 da manhã todos os dias
    @Scheduled(cron = "0 0 8 * * *")
    public void updateIndicesAutomatically() {
        orquestrarBuscaIndices();
    }
}