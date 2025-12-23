package com.gestao.financeira.dto;

import java.time.Instant;

public record MessageDTO(
        String message,
        Instant timestamp
) {
    public MessageDTO(String message) {
        this(message, Instant.now());
    }
}