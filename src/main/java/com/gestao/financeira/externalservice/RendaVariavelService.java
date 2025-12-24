package com.gestao.financeira.externalservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RendaVariavelService {

    private final String apiBrapiUrl;
    private final String apiToken;
    private final RestClient restClient;

    private Map<String, Object> cacheAcoes = new ConcurrentHashMap<>();
    private Map<String, Object> cacheFiis = new ConcurrentHashMap<>();
    private Map<String, Object> cacheEtfs = new ConcurrentHashMap<>();

    private static final String TICKERS_ACOES = "PETR4,VALE3,ITUB4,BBDC4,BBAS3,WEGE3,RENT3,BPAC11,SUZB3,PRIO3";
    private static final String TICKERS_FIIS = "MXRF11,HGLG11,KNRI11,XPLG11,VISC11,HCTR11,IRDM11,BTLG11,XPML11,VGHF11";
    private static final String TICKERS_ETFS = "BOVA11,SMAL11,IVVB11,NASD11,HASH11,XINA11,GOLD11";

    public RendaVariavelService(
            @Value("${api.brapi-url}") String apiBrapiUrl,
            @Value("${api.brapi-token}") String apiToken,
            RestClient restClient
    ) {
        this.apiBrapiUrl = apiBrapiUrl;
        this.apiToken = apiToken;
        this.restClient = restClient;
    }

    public List<Map<String, Object>> getTopAcoes() {
        return getDadosDoCacheOuApi("acoes", TICKERS_ACOES, cacheAcoes);
    }

    public List<Map<String, Object>> getTopFiis() {
        return getDadosDoCacheOuApi("fiis", TICKERS_FIIS, cacheFiis);
    }

    public List<Map<String, Object>> getTopEtfs() {
        return getDadosDoCacheOuApi("etfs", TICKERS_ETFS, cacheEtfs);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDadosDoCacheOuApi(String tipo, String tickersString, Map<String, Object> cacheAlvo) {
        if (cacheAlvo.containsKey("dados") && isCacheValid(cacheAlvo)) {
            return (List<Map<String, Object>>) cacheAlvo.get("dados");
        }
        return fetchBrapiOneByOne(tickersString, cacheAlvo);
    }

    private List<Map<String, Object>> fetchBrapiOneByOne(String tickersString, Map<String, Object> cacheParaAtualizar) {
        List<Map<String, Object>> listaFinal = new ArrayList<>();
        String[] tickersArray = tickersString.split(",");

        for (String ticker : tickersArray) {
            try {
                String url = apiBrapiUrl
                        .replace("{tickers}", ticker.trim())
                        .replace("{token}", apiToken);

                Map<String, Object> response = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (response != null && response.get("results") instanceof List<?> results) {
                    if (!results.isEmpty()) {
                        Map<String, Object> item = (Map<String, Object>) results.get(0);
                        listaFinal.add(simplificarDados(item));
                    }
                }
                // Pausa obrigatória para não ser bloqueado (Rate Limit)
                Thread.sleep(2000);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Thread interrompida durante o sleep");
            } catch (Exception e) {
                log.error("Falha no ticker {}: {}", ticker, e.getMessage());
            }
        }
        if (!listaFinal.isEmpty()) {
            atualizarCache(cacheParaAtualizar, listaFinal);
        }
        return listaFinal;
    }

    private Map<String, Object> simplificarDados(Map<String, Object> itemOriginal) {
        Map<String, Object> itemSimples = new HashMap<>();
        itemSimples.put("ticker", itemOriginal.get("symbol"));
        itemSimples.put("nome", itemOriginal.get("longName"));
        itemSimples.put("preco", itemOriginal.get("regularMarketPrice"));
        itemSimples.put("variacao_percent", itemOriginal.get("regularMarketChangePercent"));
        itemSimples.put("logo", itemOriginal.get("logourl"));
        return itemSimples;
    }

    private void atualizarCache(Map<String, Object> cache, List<Map<String, Object>> dados) {
        cache.put("dados", dados);
        cache.put("ultima_atualizacao", Instant.now().toString());
    }

    private boolean isCacheValid(Map<String, Object> cache) {
        long CACHE_MINUTES = 30;
        if (!cache.containsKey("ultima_atualizacao")) return false;
        Instant lastUpdate = Instant.parse((String) cache.get("ultima_atualizacao"));
        return Duration.between(lastUpdate, Instant.now()).toMinutes() < CACHE_MINUTES;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 10,12,14,16,18 * * MON-FRI")
    public void updateAllAutomatically() {
        // Executa sequencialmente para não sobrecarregar a rede de uma vez só
        fetchBrapiOneByOne(TICKERS_ACOES, cacheAcoes);
        fetchBrapiOneByOne(TICKERS_FIIS, cacheFiis);
        fetchBrapiOneByOne(TICKERS_ETFS, cacheEtfs);
    }

    public Map<String, Object> getCacheAcoesRaw() {
        return this.cacheAcoes;
    }

    public Map<String, Object> getCacheEtfsRaw() {
        return this.cacheEtfs;
    }

    public Map<String, Object> getCacheFiisRaw() {
        return this.cacheFiis;
    }
}