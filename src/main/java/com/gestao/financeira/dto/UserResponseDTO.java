package com.gestao.financeira.dto;

import com.gestao.financeira.entity.User;

import java.time.Instant;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}