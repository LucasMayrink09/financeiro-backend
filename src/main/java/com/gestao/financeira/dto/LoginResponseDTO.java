package com.gestao.financeira.dto;

public record LoginResponseDTO(
        String type,
        String token,
        Long expiresIn
) {
    public LoginResponseDTO(String token, Long expiresIn) {
        this("Bearer", token, expiresIn);
    }
}