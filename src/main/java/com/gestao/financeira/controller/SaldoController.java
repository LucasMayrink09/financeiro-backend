package com.gestao.financeira.controller;

import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.service.SaldoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}")
    public Saldo atualizar(@PathVariable Long id, @RequestBody Saldo saldo) {
        saldo.setId(id);
        return service.salvar(saldo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}