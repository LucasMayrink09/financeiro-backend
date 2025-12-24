package com.gestao.financeira.controller;

import com.gestao.financeira.dto.AlertaDTO;
import com.gestao.financeira.dto.AlertaResponseDTO;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.AlertaService;
import com.gestao.financeira.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService service;
    private final UserService userService;

    public AlertaController(AlertaService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Void> criar(
            @RequestBody @Valid AlertaDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        service.criarAlerta(dto, user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponseDTO>> listarMeusAlertas(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        List<AlertaResponseDTO> alertas = service.listarAlertasAtivos(user);

        return ResponseEntity.ok(alertas);
    }
}
