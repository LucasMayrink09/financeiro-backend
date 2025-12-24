package com.gestao.financeira.controller;

import com.gestao.financeira.dto.ChangePasswordDTO;
import com.gestao.financeira.dto.MessageDTO;
import com.gestao.financeira.dto.UserResponseDTO;
import com.gestao.financeira.dto.UserUpdateDTO;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(UserResponseDTO.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UserUpdateDTO data) {
        User userAtualizado = userService.updateProfile(userDetails.getUsername(), data);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(userAtualizado));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<MessageDTO> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ChangePasswordDTO data) {
        userService.changePassword(userDetails.getUsername(), data);
        return ResponseEntity.ok(new MessageDTO("Senha alterada com sucesso!"));
    }
}