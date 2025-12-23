package com.gestao.financeira.controller;

import com.gestao.financeira.dto.MessageDTO;
import com.gestao.financeira.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/ban/{id}")
    public ResponseEntity<MessageDTO> banirUsuario(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = userService.getUserIdByEmail(userDetails.getUsername());

        if (adminId.equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageDTO("Você não pode banir a si mesmo!"));
        }

        userService.banUser(id);
        return ResponseEntity.ok(new MessageDTO("Usuário banido."));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<MessageDTO> deletarUsuario(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = userService.getUserIdByEmail(userDetails.getUsername());

        if (adminId.equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageDTO("Você não pode deletar sua própria conta!"));
        }

        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageDTO("Usuário deletado."));
    }
}