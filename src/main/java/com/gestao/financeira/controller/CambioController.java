package com.gestao.financeira.controller;

import com.gestao.financeira.service.CambioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CambioController {

    @Autowired
    private CambioService cambioService;

    @GetMapping("/cambio")
    public ResponseEntity<Map<String, Object>> getCotacaoDolar() {
        Map<String, Object> cotacao = cambioService.buscarCotacaoAtualizada();
        return ResponseEntity.ok(cotacao);
    }
}
