package com.gestao.financeira.dto;

public record ForgotPasswordResponseDTO(
        String message,
        String resetCode
) {}
