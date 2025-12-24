package com.gestao.financeira.service;

import com.gestao.financeira.dto.AlertaDTO;
import com.gestao.financeira.dto.AlertaResponseDTO;
import com.gestao.financeira.entity.Alerta;
import com.gestao.financeira.entity.Moeda;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.exception.RegraDeNegocioException;
import com.gestao.financeira.externalservice.CriptoService;
import com.gestao.financeira.externalservice.RendaVariavelService;
import com.gestao.financeira.repository.AlertaRepository;
import com.gestao.financeira.externalservice.CambioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlertaService {

    private final AlertaRepository repository;
    private final EmailService emailService;
    private final RendaVariavelService rvService;
    private final CriptoService criptoService;
    private final CambioService cambioService;

    private static final long MINUTOS_ENTRE_CRIACOES = 120;
    private static final long MAX_ALERTAS_ATIVOS = 3;

    public AlertaService(AlertaRepository repository, EmailService emailService,
                         RendaVariavelService rvService, CriptoService criptoService,
                         CambioService cambioService) {
        this.repository = repository;
        this.emailService = emailService;
        this.rvService = rvService;
        this.criptoService = criptoService;
        this.cambioService = cambioService;
    }

    @Transactional
    public void criarAlerta(AlertaDTO dto, User user) {
        validarQuantidadeMaxima(user);
        validarRateLimitTempo(user);

        String ticker = dto.ticker().toUpperCase().trim();
        BigDecimal precoAtual = obterPrecoAtualNaMoeda(ticker, dto.moeda());

        if (precoAtual == null) {
            throw new RegraDeNegocioException("Ativo não encontrado ou sem cotação: " + ticker);
        }

        Alerta alerta = new Alerta();
        alerta.setUser(user);
        alerta.setTicker(ticker);
        alerta.setPrecoAlvo(dto.precoAlvo());
        alerta.setMoeda(dto.moeda());

        if (BigDecimal.valueOf(dto.precoAlvo()).compareTo(precoAtual) > 0) {
            alerta.setCondicao("MAIOR");
        } else {
            alerta.setCondicao("MENOR");
        }

        repository.save(alerta);
        if (precoAtual != null && deveDisparar(alerta, precoAtual)) {
            dispararNotificacao(alerta, precoAtual);
        }

        log.info("Alerta criado: {} quer {} em {} {} (Atual: {})",
                user.getEmail(), ticker, dto.moeda(), dto.precoAlvo(), precoAtual);
    }

    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertasAtivos(User user) {
        return repository.findByUserIdAndDisparadoFalse(user.getId())
                .stream()
                .map(AlertaResponseDTO::fromEntity)
                .toList();
    }

    private void validarQuantidadeMaxima(User user) {
        long ativos = repository.countByUserIdAndDisparadoFalse(user.getId());
        if (ativos >= MAX_ALERTAS_ATIVOS) {
            throw new RegraDeNegocioException("Você atingiu o limite de " + MAX_ALERTAS_ATIVOS + " alertas ativos.");
        }
    }

    private void validarRateLimitTempo(User user) {
        repository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .ifPresent(ultimoAlerta -> {
                    long minutosPassados = Duration.between(ultimoAlerta.getCreatedAt(), Instant.now()).toMinutes();
                    if (minutosPassados < MINUTOS_ENTRE_CRIACOES) {
                        throw new RegraDeNegocioException("Aguarde alguns minutos para criar um novo alerta.");
                    }
                });
    }

    @Scheduled(fixedRate = 7200000)
    public void processarAlertas() {
        List<Alerta> alertasAtivos = repository.findTop100ByDisparadoFalse();
        if (alertasAtivos.isEmpty()) return;

        log.info("Processando {} alertas ativos...", alertasAtivos.size());

        Map<String, CotacaoDual> cotacoesMercado = obterCotacoesCompletas();

        for (Alerta alerta : alertasAtivos) {
            CotacaoDual cotacao = cotacoesMercado.get(alerta.getTicker());

            if (cotacao != null) {
                BigDecimal valorAtual = (alerta.getMoeda() == Moeda.USD) ? cotacao.valorUsd() : cotacao.valorBrl();

                if (deveDisparar(alerta, valorAtual)) {
                    dispararNotificacao(alerta, valorAtual);
                }
            }
        }
    }

    private boolean deveDisparar(Alerta alerta, BigDecimal atual) {
        BigDecimal alvo = BigDecimal.valueOf(alerta.getPrecoAlvo());

        if ("MAIOR".equals(alerta.getCondicao())) {
            return atual.compareTo(alvo) >= 0;
        } else {
            return atual.compareTo(alvo) <= 0;
        }
    }

    private void dispararNotificacao(Alerta alerta, BigDecimal precoAtual) {
        try {
            String simbolo = (alerta.getMoeda() == Moeda.USD) ? "US$" : "R$";
            String assunto = "Alerta: " + alerta.getTicker() + " atingiu " + simbolo + " " + precoAtual;

            String corpo = String.format(
                    """
                    <h2>Alerta de Preço Disparado!</h2>
                    <p>O ativo <b>%s</b> chegou no valor de <b>%s %s</b>.</p>
                    <p>Seu objetivo era: %s %d</p>
                    <hr/>
                    <p><small>Este alerta foi desativado automaticamente.</small></p>
                    """,
                    alerta.getTicker(), simbolo, precoAtual, simbolo, alerta.getPrecoAlvo()
            );

            emailService.sendHtmlEmail(alerta.getUser().getEmail(), assunto, corpo);

            alerta.setDisparado(true);
            repository.save(alerta);

        } catch (Exception e) {
            log.error("Erro ao notificar alerta {}: {}", alerta.getId(), e.getMessage());
        }
    }

    record CotacaoDual(BigDecimal valorBrl, BigDecimal valorUsd) {}

    private BigDecimal obterPrecoAtualNaMoeda(String ticker, Moeda moeda) {
        Map<String, CotacaoDual> mercado = obterCotacoesCompletas();
        CotacaoDual cotacao = mercado.get(ticker);
        if (cotacao == null) return null;
        return (moeda == Moeda.USD) ? cotacao.valorUsd() : cotacao.valorBrl();
    }

    private Map<String, CotacaoDual> obterCotacoesCompletas() {
        Map<String, CotacaoDual> mercado = new HashMap<>();

        Double dolarHoje = (Double) cambioService.buscarCotacaoAtualizada().get("cotacao");
        if (dolarHoje == null) dolarHoje = 5.50;
        BigDecimal taxaDolar = BigDecimal.valueOf(dolarHoje);

        processarRendaVariavel(rvService.getCacheAcoesRaw(), mercado, taxaDolar);
        processarRendaVariavel(rvService.getCacheFiisRaw(), mercado, taxaDolar);
        processarRendaVariavel(rvService.getCacheEtfsRaw(), mercado, taxaDolar);

        Map<String, Object> criptoCache = criptoService.getCacheRaw();
        if (criptoCache != null && criptoCache.containsKey("lista_moedas")) {
            List<Map<String, Object>> dados = (List<Map<String, Object>>) criptoCache.get("lista_moedas");
            for (Map<String, Object> item : dados) {
                String t = (String) item.get("simbolo");
                Double valUsd = (Double) item.get("cotacao_em_dolar");
                Double valBrl = (Double) item.get("cotacao_em_reais");

                if (t != null && valUsd != null && valBrl != null) {
                    mercado.put(t, new CotacaoDual(BigDecimal.valueOf(valBrl), BigDecimal.valueOf(valUsd)));
                }
            }
        }
        return mercado;
    }

    @SuppressWarnings("unchecked")
    private void processarRendaVariavel(Map<String, Object> cache, Map<String, CotacaoDual> mercado, BigDecimal taxaDolar) {
        if (cache != null && cache.containsKey("dados")) {
            List<Map<String, Object>> dados = (List<Map<String, Object>>) cache.get("dados");
            for (Map<String, Object> item : dados) {
                String ticker = (String) item.get("ticker");
                Double precoBrl = (Double) item.get("preco");

                if (ticker != null && precoBrl != null) {
                    BigDecimal brl = BigDecimal.valueOf(precoBrl);
                    BigDecimal usd = brl.divide(taxaDolar, 2, RoundingMode.HALF_UP);

                    mercado.put(ticker, new CotacaoDual(brl, usd));
                }
            }
        }
    }
}