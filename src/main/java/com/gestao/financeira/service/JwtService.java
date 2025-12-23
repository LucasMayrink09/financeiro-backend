package com.gestao.financeira.service;

import com.gestao.financeira.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final Key key;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long expiration
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret deve ter no mínimo 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(User user) {
        long lastPwdChange = user.getLastPasswordChange() != null ?
                user.getLastPasswordChange().toEpochMilli() :
                0L;

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .claim("pwd", lastPwdChange)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long extractPwdChange(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("pwd", Long.class);
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException e) {
            throw new BadCredentialsException("Token inválido ou expirado");
        }
    }
}