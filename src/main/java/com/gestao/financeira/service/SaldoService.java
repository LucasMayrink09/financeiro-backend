package com.gestao.financeira.service;

import com.gestao.financeira.dto.SaldoRequestDTO;
import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.exception.RegraDeNegocioException;
import com.gestao.financeira.repository.SaldoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SaldoService {

    private final SaldoRepository repository;

    public SaldoService(SaldoRepository repository) {
        this.repository = repository;
    }

    public List<Saldo> listarDoUsuario(User user) {
        return repository.findByUserId(user.getId());
    }

    public Saldo salvar(SaldoRequestDTO dto, User user) {
        Saldo saldo = new Saldo();
        saldo.setUser(user);
        preencherCamposBasicos(saldo, dto);
        preencherCamposFinanceiros(saldo, dto);
        return repository.save(saldo);
    }

    public Saldo atualizar(Long id, SaldoRequestDTO dto, User user) {
        Saldo existente = findSaldoDoUsuario(id, user);
        preencherCamposBasicos(existente, dto);
        preencherCamposFinanceiros(existente, dto);
        return repository.save(existente);
    }

    public void deletar(Long saldoId, User user) {
        Saldo saldo = findSaldoDoUsuario(saldoId, user);
        repository.delete(saldo);
    }

    private Saldo findSaldoDoUsuario(Long id, User user) {
        return repository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RegraDeNegocioException("Saldo não encontrado"));
    }

    private boolean isRendaVariavel(SaldoRequestDTO dto) {
        // Se tem símbolo e quantidade, consideramos Renda Variável (Ação, FII, Cripto)
        return dto.simbolo() != null && dto.quantidade() != null;
    }

    private void preencherCamposBasicos(Saldo saldo, SaldoRequestDTO dto) {
        saldo.setNomeConta(dto.nomeConta());
        saldo.setTipo(dto.tipo());
        saldo.setData(dto.data());
        saldo.setObservacao(dto.observacao());
    }

    private void preencherCamposFinanceiros(Saldo saldo, SaldoRequestDTO dto) {
        if (isRendaVariavel(dto)) {
            // Lógica para Renda Variável (Ações, Cripto, FIIs)
            saldo.setSimbolo(dto.simbolo());
            saldo.setQuantidade(BigDecimal.valueOf(dto.quantidade()));
            saldo.setTaxa(null);
            if (dto.valor() != null) {
                saldo.setValor(BigDecimal.valueOf(dto.valor()));
                saldo.setMoeda(dto.moeda());
            } else {
                if (saldo.getId() == null) {
                    saldo.setValor(null);
                    saldo.setMoeda(null);
                }
            }
        } else {
            if (dto.valor() == null) {
                throw new RegraDeNegocioException("Valor é obrigatório para renda fixa");
            }

            saldo.setValor(BigDecimal.valueOf(dto.valor()));
            saldo.setMoeda(dto.moeda());
            saldo.setTaxa(dto.taxa() != null ? BigDecimal.valueOf(dto.taxa()) : null);
            saldo.setSimbolo(null);
            saldo.setQuantidade(null);
        }
    }
}