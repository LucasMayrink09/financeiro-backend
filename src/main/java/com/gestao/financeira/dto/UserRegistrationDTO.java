package com.gestao.financeira.dto;

import com.gestao.financeira.security.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @StrongPassword
        String password
) {}
