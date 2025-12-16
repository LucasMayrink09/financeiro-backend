package com.gestao.financeira.service;

import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.repository.SaldoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaldoService {

    private final SaldoRepository repository;

    public SaldoService(SaldoRepository repository) {
        this.repository = repository;
    }

    public List<Saldo> listar() {
        return repository.findAll();
    }

    public Saldo salvar(Saldo saldo) {
        normalizarSaldo(saldo);
        return repository.save(saldo);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private void normalizarSaldo(Saldo saldo) {

        boolean isCripto = saldo.getSimbolo() != null
                && saldo.getQuantidade() != null;

        if (isCripto) {
            saldo.setValor(null);
            saldo.setMoeda(null);
        } else {
            saldo.setSimbolo(null);
            saldo.setQuantidade(null);
        }
    }
}