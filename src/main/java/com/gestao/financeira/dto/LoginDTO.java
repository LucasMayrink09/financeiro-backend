package com.gestao.financeira.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 254)
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 128)
        String password
) {}
