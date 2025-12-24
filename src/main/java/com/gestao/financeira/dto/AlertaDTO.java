package com.gestao.financeira.dto;

import com.gestao.financeira.entity.Moeda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlertaDTO(
        @NotBlank(message = "Ticker é obrigatório")
        String ticker,

        @NotNull(message = "Preço alvo é obrigatório")
        @Positive(message = "Preço deve ser positivo")
        Long precoAlvo,

        @NotNull(message = "Moeda é obrigatória (BRL ou USD)")
        Moeda moeda
) {}