package com.gestao.financeira.controller;

import com.gestao.financeira.entity.Meta;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.service.MetaService;
import com.gestao.financeira.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaService metaService;
    private final UserService userService;

    public MetaController(MetaService metaService, UserService userService) {
        this.metaService = metaService;
        this.userService = userService;
    }

    @GetMapping
    public Meta buscar(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        return metaService.buscarDoUsuario(user);
    }

    @PutMapping
    public Meta salvar(@RequestBody Meta meta,
                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByEmailOrThrow(userDetails.getUsername());
        return metaService.salvar(meta, user);
    }
}

