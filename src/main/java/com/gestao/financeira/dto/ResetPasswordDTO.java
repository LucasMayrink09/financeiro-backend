package com.gestao.financeira.dto;

import com.gestao.financeira.security.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDTO(
        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @StrongPassword
        String newPassword
) {}
