package com.gestao.financeira.externalservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class CriptoService {

    private final String apiCmcUrl;
    private final String apiCmcKey;
    private final String apiCoinGeckoUrl;
    private final CambioService cambioService;
    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build();
    private Map<String, Object> cacheCripto = new ConcurrentHashMap<>();

    private Instant momentoUltimoErroPrincipal = null;
    private int errosConsecutivos = 0;
    private static final int LIMITE_ERROS_PARA_BLOQUEAR = 3;
    private static final long TEMPO_ESPERA_MINUTOS = 60;

    private static final String MOEDAS_ALVO_CMC = "BTC,ETH,XRP,BNB,SOL,USDC,TRX,DOGE,ADA,USDT";

    public CriptoService(
            @Value("${api.cmc-url}") String apiCmcUrl,
            @Value("${api.cmc-key}") String apiCmcKey,
            @Value("${api.coingecko-url}") String apiCoinGeckoUrl,
            CambioService cambioService
    ) {
        this.apiCmcUrl = apiCmcUrl;
        this.apiCmcKey = apiCmcKey;
        this.apiCoinGeckoUrl = apiCoinGeckoUrl;
        this.cambioService = cambioService;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buscarTop10Criptos() {
        if (cacheCripto.containsKey("lista_moedas") && isCacheValid()) {
            return (List<Map<String, Object>>) cacheCripto.get("lista_moedas");
        }
        return orquestrarBuscaDeDados();
    }

    private List<Map<String, Object>> orquestrarBuscaDeDados() {
        Double cotacaoDolar = obterDolarAtual();
        if (cotacaoDolar == null) return obterFallbackEmergencia();

        if (isPrincipalBloqueada()) {
            log.warn("CoinMarketCap em tempo de espera. Usando CoinGecko (Fallback).");
            return fallbackCoinGecko(cotacaoDolar);
        }

        try {
            log.info("Tentando buscar via CoinMarketCap (Principal)...");
            List<Map<String, Object>> resultado = tentarCoinMarketCap(cotacaoDolar);
            errosConsecutivos = 0;
            momentoUltimoErroPrincipal = null;
            return resultado;

        } catch (Exception e) {
            errosConsecutivos++;
            log.warn("Falha ao consultar CoinMarketCap (Erro {}/{}): {}",
                    errosConsecutivos, LIMITE_ERROS_PARA_BLOQUEAR, e.getMessage());

            if (errosConsecutivos >= LIMITE_ERROS_PARA_BLOQUEAR) {
                momentoUltimoErroPrincipal = Instant.now();
                log.error("Limite de erros atingido. Bloqueando CoinMarketCap por {} minutos.", TEMPO_ESPERA_MINUTOS);
            }

            return fallbackCoinGecko(cotacaoDolar);
        }
    }

    private boolean isPrincipalBloqueada() {
        if (momentoUltimoErroPrincipal == null) return false;
        long minutosPassados = Duration.between(momentoUltimoErroPrincipal, Instant.now()).toMinutes();
        return minutosPassados < TEMPO_ESPERA_MINUTOS;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tentarCoinMarketCap(Double cotacaoDolar) {
        String urlCompleta = apiCmcUrl + "?symbol=" + MOEDAS_ALVO_CMC + "&convert=USD";

        Map<String, Object> response = restClient.get()
                .uri(urlCompleta)
                .header("X-CMC_PRO_API_KEY", apiCmcKey)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null || !response.containsKey("data")) {
            throw new RuntimeException("Resposta inválida da CMC");
        }
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> processada = new ArrayList<>();
        String[] simbolosOrdenados = MOEDAS_ALVO_CMC.split(",");

        for (String targetSymbol : simbolosOrdenados) {
            if (dataMap.containsKey(targetSymbol)) {
                Map<String, Object> coinData = (Map<String, Object>) dataMap.get(targetSymbol);

                String symbol = (String) coinData.get("symbol");
                String name = (String) coinData.get("name");

                Map<String, Object> quote = (Map<String, Object>) coinData.get("quote");
                Map<String, Object> usdInfo = (Map<String, Object>) quote.get("USD");
                Double priceUsd = ((Number) usdInfo.get("price")).doubleValue();

                processada.add(montarObjetoMoeda(symbol, name, priceUsd, cotacaoDolar));
            }
        }

        atualizarCache(processada);
        return processada;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fallbackCoinGecko(Double cotacaoDolar) {
        try {
            List<Map<String, Object>> listaBruta = restClient.get()
                    .uri(apiCoinGeckoUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            List<Map<String, Object>> processada = new ArrayList<>();

            if (listaBruta != null) {
                for (Map<String, Object> item : listaBruta) {
                    String symbol = ((String) item.get("symbol")).toUpperCase();
                    String name = (String) item.get("name");
                    Double priceUsd = ((Number) item.get("current_price")).doubleValue();

                    processada.add(montarObjetoMoeda(symbol, name, priceUsd, cotacaoDolar));
                }
            }

            atualizarCache(processada);
            return processada;

        } catch (Exception e) {
            log.error("Erro Crítico: CoinGecko também falhou: {}", e.getMessage());
            return obterFallbackEmergencia();
        }
    }

    private Double obterDolarAtual() {
        Map<String, Object> dadosDolar = cambioService.buscarCotacaoAtualizada();
        if (dadosDolar != null && dadosDolar.get("cotacao") != null) {
            return (Double) dadosDolar.get("cotacao");
        }
        return null;
    }

    private Map<String, Object> montarObjetoMoeda(String simbolo, String nomeBase, Double valorEmDolar, Double cotacaoDolar) {
        Map<String, Object> moeda = new HashMap<>();
        moeda.put("nome", nomeBase);
        moeda.put("simbolo", simbolo);
        moeda.putAll(calcularPrecos(valorEmDolar, cotacaoDolar));
        return moeda;
    }

    private Map<String, Double> calcularPrecos(Double valorOriginalUsd, Double taxaCambioBrl) {
        Map<String, Double> valores = new HashMap<>();
        valores.put("cotacao_em_dolar", valorOriginalUsd);
        valores.put("cotacao_em_reais", valorOriginalUsd * taxaCambioBrl);
        return valores;
    }

    private void atualizarCache(List<Map<String, Object>> lista) {
        Map<String, Object> novoCache = new ConcurrentHashMap<>();
        novoCache.put("lista_moedas", lista);
        novoCache.put("ultima_atualizacao", Instant.now().toString());
        this.cacheCripto = novoCache;
    }

    private boolean isCacheValid() {
        long CACHE_DURATION_MINUTES = 30;
        if (!cacheCripto.containsKey("ultima_atualizacao")) return false;
        Instant lastUpdate = Instant.parse((String) cacheCripto.get("ultima_atualizacao"));
        long elapsedMinutes = java.time.Duration.between(lastUpdate, Instant.now()).toMinutes();
        return elapsedMinutes < CACHE_DURATION_MINUTES;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obterFallbackEmergencia() {
        if (cacheCripto.containsKey("lista_moedas")) {
            return (List<Map<String, Object>>) cacheCripto.get("lista_moedas");
        }
        return List.of(montarObjetoMoeda("BTC", "Bitcoin", 98000.0, 6.0));
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void updateCriptosAutomatically() {
        orquestrarBuscaDeDados();
    }

    public Map<String, Object> getCacheRaw() {
        return this.cacheCripto;
    }
}