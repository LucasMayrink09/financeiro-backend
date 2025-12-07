package com.gestao.financeira.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("message", "API de Gestão Financeira está online! 🚀");
    }
}