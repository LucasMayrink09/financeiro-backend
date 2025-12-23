package com.gestao.financeira.controller;

import com.gestao.financeira.dto.SaldoRequestDTO;
import com.gestao.financeira.dto.SaldoResponseDTO;
import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.SaldoService;
import com.gestao.financeira.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saldos")
public class SaldoController {

    private final SaldoService saldoService;
    private final UserService userService;

    public SaldoController(SaldoService saldoService, UserService userService) {
        this.saldoService = saldoService;
        this.userService = userService;
    }

    @GetMapping
    public List<SaldoResponseDTO> listar(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        return saldoService.listarDoUsuario(user)
                .stream()
                .map(SaldoResponseDTO::fromEntity)
                .toList();
    }

    @PostMapping
    public SaldoResponseDTO salvar(@RequestBody @Valid SaldoRequestDTO dto,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        Saldo saldoSalvo = saldoService.salvar(dto, user);
        return SaldoResponseDTO.fromEntity(saldoSalvo);
    }

    @PutMapping("/{id}")
    public SaldoResponseDTO atualizar(@PathVariable Long id,
                                      @RequestBody @Valid SaldoRequestDTO dto,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        Saldo saldoAtualizado = saldoService.atualizar(id, dto, user);
        return SaldoResponseDTO.fromEntity(saldoAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        saldoService.deletar(id, user);
    }
}