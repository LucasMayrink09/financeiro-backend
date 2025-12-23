package com.gestao.financeira.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}