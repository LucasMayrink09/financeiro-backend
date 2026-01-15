package com.gestao.financeira.dto;

public record SocialUserInfo(
        String email,
        String name,
        SocialProvider provider
) {}