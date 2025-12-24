package com.gestao.financeira.dto;

import com.gestao.financeira.entity.Alerta;
import com.gestao.financeira.entity.Moeda;

public record AlertaResponseDTO(
        Long id,
        String ticker,
        Long precoAlvo,
        Moeda moeda,
        String condicao
) {
    public static AlertaResponseDTO fromEntity(Alerta alerta) {
        return new AlertaResponseDTO(
                alerta.getId(),
                alerta.getTicker(),
                alerta.getPrecoAlvo(),
                alerta.getMoeda(),
                alerta.getCondicao()
        );
    }
}
