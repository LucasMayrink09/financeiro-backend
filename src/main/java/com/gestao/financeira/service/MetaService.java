package com.gestao.financeira.service;

import com.gestao.financeira.entity.Meta;
import com.gestao.financeira.repository.MetaRepository;
import org.springframework.stereotype.Service;

@Service
public class MetaService {

    private final MetaRepository repository;

    public MetaService(MetaRepository repository) {
        this.repository = repository;
    }

    public Meta buscar() {
        return repository.findById(1L)
                .orElse(new Meta());
    }

    public Meta salvar(Meta meta) {
        meta.setId(1L);
        return repository.save(meta);
    }
}
