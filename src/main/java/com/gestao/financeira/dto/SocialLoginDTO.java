package com.gestao.financeira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginDTO(
        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotNull(message = "Provedor é obrigatório")
        SocialProvider provider
) {}