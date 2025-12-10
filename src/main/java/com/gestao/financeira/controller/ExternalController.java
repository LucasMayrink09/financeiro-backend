package com.gestao.financeira.controller;

import com.gestao.financeira.service.CriptoService;
import com.gestao.financeira.service.CambioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExternalController {

    private final CambioService cambioService;
    private final CriptoService criptoService;

    public ExternalController(CambioService cambioService, CriptoService criptoService) {
        this.cambioService = cambioService;
        this.criptoService = criptoService;
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
}
