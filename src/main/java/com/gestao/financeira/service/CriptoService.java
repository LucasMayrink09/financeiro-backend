package com.gestao.financeira.service;

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

    private final String apiCoinCapUrl;
    private final String apiBinanceUrl;
    private final CambioService cambioService;
    private final RestClient restClient = RestClient.create();

    private Map<String, Object> cacheCripto = new ConcurrentHashMap<>();

    private Instant momentoUltimoErroBinance = null;
    private static final long TEMPO_ESPERA_MINUTOS = 60;

    private static final List<String> MOEDAS_ALVO_BINANCE = List.of(
            "BTCUSDT", "ETHUSDT", "XRPUSDT", "BNBUSDT",
            "SOLUSDT", "USDCUSDT", "TRXUSDT", "DOGEUSDT", "ADAUSDT"
    );

    public CriptoService(
            @Value("${api.coincap-url}") String apiCoinCapUrl,
            @Value("${api.binance-url}") String apiBinanceUrl,
            CambioService cambioService
    ) {
        this.apiCoinCapUrl = apiCoinCapUrl;
        this.apiBinanceUrl = apiBinanceUrl;
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

        if (isBinanceBloqueada()) {
            log.error("Binance em tempo de espera. Usando CoinCap (Fallback).");
            return fallbackCoinCap(cotacaoDolar);
        }

        try {
            log.info("Tentando buscar via Binance...");
            List<Map<String, Object>> resultado = tentarBinance(cotacaoDolar);

            momentoUltimoErroBinance = null;
            return resultado;

        } catch (Exception e) {
            momentoUltimoErroBinance = Instant.now();
            log.error("Falha na Binance: {}", e.getMessage());
            return fallbackCoinCap(cotacaoDolar);
        }
    }

    private boolean isBinanceBloqueada() {
        if (momentoUltimoErroBinance == null) return false;
        long minutosPassados = Duration.between(momentoUltimoErroBinance, Instant.now()).toMinutes();
        return minutosPassados < TEMPO_ESPERA_MINUTOS;
    }


    private List<Map<String, Object>> tentarBinance(Double cotacaoDolar) {
        List<Map<String, Object>> todasCotacoes = restClient.get()
                .uri(apiBinanceUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        List<Map<String, Object>> processada = new ArrayList<>();

        processada.add(montarObjetoMoeda("USDT", "Tether", 1.0, cotacaoDolar));

        if (todasCotacoes != null) {
            for (Map<String, Object> item : todasCotacoes) {
                String symbol = (String) item.get("symbol");

                if (MOEDAS_ALVO_BINANCE.contains(symbol)) {
                    String priceStr = (String) item.get("price");
                    Double priceUsd = Double.parseDouble(priceStr);

                    String symbolLimpo = symbol.replace("USDT", "");
                    processada.add(montarObjetoMoeda(symbolLimpo, obterNomeBase(symbolLimpo), priceUsd, cotacaoDolar));
                }
            }
        }
        atualizarCache(processada);
        return processada;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fallbackCoinCap(Double cotacaoDolar) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(apiCoinCapUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !response.containsKey("data")) {
                throw new RuntimeException("Resposta vazia");
            }

            List<Map<String, Object>> listaBruta = (List<Map<String, Object>>) response.get("data");
            List<Map<String, Object>> processada = new ArrayList<>();

            for (Map<String, Object> item : listaBruta) {
                String symbol = (String) item.get("symbol");
                String name = (String) item.get("name");
                String priceStr = (String) item.get("priceUsd");

                Double priceUsd = Double.parseDouble(priceStr);
                processada.add(montarObjetoMoeda(symbol, name, priceUsd, cotacaoDolar));
            }

            atualizarCache(processada);
            return processada;

        } catch (Exception e) {
            log.error("Erro Crítico: CoinCap também falhou: " + e.getMessage());
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
        Map<String, Object> novoCache = new HashMap<>();
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

    private String obterNomeBase(String simbolo) {
        return switch (simbolo) {
            case "BTC" -> "Bitcoin";
            case "ETH" -> "Ethereum";
            case "USDT" -> "Tether";
            case "XRP" -> "XRP";
            case "BNB" -> "Binance Coin";
            case "SOL" -> "Solana";
            case "USDC" -> "USD Coin";
            case "TRX" -> "TRON";
            case "DOGE" -> "Dogecoin";
            case "ADA" -> "Cardano";
            default -> simbolo;
        };
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void updateCriptosAutomatically() {
        orquestrarBuscaDeDados();
    }
}