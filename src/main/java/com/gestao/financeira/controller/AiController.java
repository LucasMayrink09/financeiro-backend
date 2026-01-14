package com.gestao.financeira.controller;

import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.AiService;
import com.gestao.financeira.service.SaldoService;
import com.gestao.financeira.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    public AiController(AiService aiService, UserService userService) {
        this.aiService = aiService;
        this.userService = userService;
    }

    @PostMapping("/analise")
    public ResponseEntity<Map<String, String>> analisarCarteira(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        String analise = aiService.analisarInvestimentos(user);
        return ResponseEntity.ok(Map.of("analise", analise));
    }
}