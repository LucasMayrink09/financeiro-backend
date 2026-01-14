package com.gestao.financeira.controller;

import com.gestao.financeira.dto.*;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.AuthService;
import com.gestao.financeira.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO data) {
        LoginResponseDTO response = authService.login(data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid UserRegistrationDTO data) {
        String token = authService.register(data);
        RegisterResponseDTO response = new RegisterResponseDTO(
                "Usuário registrado! Verifique seu email.",
                token
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(@RequestBody @Valid ForgotPasswordDTO data) {
        String code = authService.forgotPassword(data.email());

        if (code == null) {
            return ResponseEntity.ok(new ForgotPasswordResponseDTO(
                    "Se o email existir, você receberá instruções.",
                    null
            ));
        }
        return ResponseEntity.ok(new ForgotPasswordResponseDTO(
                "Código gerado com sucesso.",
                code
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageDTO> resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        authService.resetPassword(data);
        return ResponseEntity.ok(new MessageDTO("Senha redefinida com sucesso!"));
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<LoginResponseDTO> confirmEmail(@RequestParam String token) {
        User user = authService.confirmEmail(token);
        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(jwtToken, 3600L));
    }
}