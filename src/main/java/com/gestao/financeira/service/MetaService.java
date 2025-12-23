package com.gestao.financeira.service;

import com.gestao.financeira.entity.Meta;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.repository.MetaRepository;
import org.springframework.stereotype.Service;

@Service
public class MetaService {

    private final MetaRepository repository;

    public MetaService(MetaRepository repository) {
        this.repository = repository;
    }

    public Meta buscarDoUsuario(User user) {
        return repository.findByUserId(user.getId())
                .orElse(null);
    }

    public Meta salvar(Meta meta, User user) {
        Meta existente = repository.findByUserId(user.getId())
                .orElse(null);

        if (existente != null) {
            existente.setValor(meta.getValor());
            existente.setMoeda(meta.getMoeda());
            return repository.save(existente);
        }

        meta.setUser(user);
        return repository.save(meta);
    }
}