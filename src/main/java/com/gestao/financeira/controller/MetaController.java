package com.gestao.financeira.controller;

import com.gestao.financeira.entity.Meta;
import com.gestao.financeira.service.MetaService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaService service;

    public MetaController(MetaService service) {
        this.service = service;
    }

    @GetMapping("/saldo")
    public Meta buscar() {
        return service.buscar();
    }

    @PutMapping("/saldo")
    public Meta salvar(@RequestBody Meta meta) {
        return service.salvar(meta);
    }
}

