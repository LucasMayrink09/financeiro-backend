package com.gestao.financeira.controller;

import com.gestao.financeira.externalservice.CriptoService;
import com.gestao.financeira.externalservice.CambioService;
import com.gestao.financeira.externalservice.RendaFixaService;
import com.gestao.financeira.externalservice.RendaVariavelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExternalController {

    private final CambioService cambioService;
    private final CriptoService criptoService;
    private final RendaFixaService rendaFixaService;
    private final RendaVariavelService rendaVariavelService;

    public ExternalController(
            CambioService cambioService,
            CriptoService criptoService,
            RendaFixaService rendaFixaService,
            RendaVariavelService rendaVariavelService
    ) {
        this.cambioService = cambioService;
        this.criptoService = criptoService;
        this.rendaFixaService = rendaFixaService;
        this.rendaVariavelService = rendaVariavelService;
    }

    @GetMapping("/cambio")
    public ResponseEntity<Map<String, Object>> getCotacaoDolar() {
        Map<String, Object> cotacao = cambioService.buscarCotacaoAtualizada();
        return ResponseEntity.ok(cotacao);
    }

    @GetMapping("/criptos")
    public ResponseEntity<List<Map<String, Object>>> getCriptos() {
        return ResponseEntity.ok(criptoService.buscarTop10Criptos());
    }

    /**
     * Retorna os índices de mercado (CDI, SELIC, IPCA).
     * O Frontend usa isso para mostrar "CDI Hoje: 13.65%" na tela.
     */
    @GetMapping("/indices")
    public ResponseEntity<Map<String, Object>> getIndicesFinanceiros() {
        return ResponseEntity.ok(rendaFixaService.buscarIndicesAtuais());
    }

    /**
     * Simula quanto um valor rendeu HOJE.
     * Exemplo de chamada: /api/simular-cdi?saldo=1000&percentual=110
     */
    @GetMapping("/simular-cdi")
    public ResponseEntity<BigDecimal> getSimulacaoRendaFixa(
            @RequestParam BigDecimal saldo,
            @RequestParam BigDecimal percentual) {
        //  O service faz a conta e retorna o valor (ex: 0.45)
        // O Frontend pega esse valor e soma ao saldo visualmente
        return ResponseEntity.ok(rendaFixaService.calcularRendimentoDiario(saldo, percentual));
    }

    // --- Renda Variável
    @GetMapping("/rv/acoes")
    public ResponseEntity<List<Map<String, Object>>> getTopAcoes() {
        // Retorna a lista de Ações (PETR4, VALE3...) já formatada
        return ResponseEntity.ok(rendaVariavelService.getTopAcoes());
    }

    @GetMapping("/rv/fiis")
    public ResponseEntity<List<Map<String, Object>>> getTopFiis() {
        // Retorna a lista de FIIs (MXRF11, HGLG11...)
        return ResponseEntity.ok(rendaVariavelService.getTopFiis());
    }

    @GetMapping("/rv/etfs")
    public ResponseEntity<List<Map<String, Object>>> getTopEtfs() {
        // Retorna a lista de ETFs (IVVB11, BOVA11...)
        return ResponseEntity.ok(rendaVariavelService.getTopEtfs());
    }
}
