package com.gestao.financeira.repository;

import com.gestao.financeira.entity.Meta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaRepository extends JpaRepository<Meta, Long> {
    Optional<Meta> findByUserId(Long userId);
}
