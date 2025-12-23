package com.gestao.financeira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record SaldoRequestDTO(
        @NotBlank(message = "Nome da conta é obrigatório")
        String nomeConta,

        @PositiveOrZero(message = "O valor não pode ser negativo")
        Double valor,

        String moeda,

        @NotBlank(message = "Tipo é obrigatório")
        String tipo, // "ACAO", "FII", "CRIPTO", "RENDA_FIXA"

        String simbolo, // Para ações/cripto

        @PositiveOrZero
        Double quantidade, // Para ações/cripto

        @PositiveOrZero
        Double taxa, // Para renda fixa (% CDI)

        @NotNull(message = "Data é obrigatória")
        LocalDate data,

        String observacao
) {}