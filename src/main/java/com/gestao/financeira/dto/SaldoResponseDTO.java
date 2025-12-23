package com.gestao.financeira.dto;

import com.gestao.financeira.entity.Saldo;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaldoResponseDTO(
        Long id,
        String nomeConta,
        BigDecimal valor,
        String moeda,
        String tipo,
        String simbolo,
        BigDecimal quantidade,
        BigDecimal taxa,
        LocalDate data,
        String observacao
) {
    public static SaldoResponseDTO fromEntity(Saldo saldo) {
        return new SaldoResponseDTO(
                saldo.getId(),
                saldo.getNomeConta(),
                saldo.getValor(),
                saldo.getMoeda(),
                saldo.getTipo(),
                saldo.getSimbolo(),
                saldo.getQuantidade(),
                saldo.getTaxa(),
                saldo.getData(),
                saldo.getObservacao()
        );
    }
}