package com.gestao.financeira.controller;

import com.gestao.financeira.model.Saldo;
import com.gestao.financeira.service.SaldoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saldos")
public class SaldoController {

    private final SaldoService service;

    public SaldoController(SaldoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Saldo> listar() {
        return service.listar();
    }

    @PostMapping
    public Saldo salvar(@RequestBody Saldo saldo) {
        return service.salvar(saldo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
