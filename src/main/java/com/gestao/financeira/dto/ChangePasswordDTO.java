package com.gestao.financeira.dto;

import com.gestao.financeira.security.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "Nova senha é obrigatória")
        @StrongPassword
        String newPassword
) {}