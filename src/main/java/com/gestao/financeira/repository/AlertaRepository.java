package com.gestao.financeira.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findTop100ByDisparadoFalse();
    // Para listar pro usuário no front
    List<Alerta> findByUserIdAndDisparadoFalse(Long userId);
    Optional<Alerta> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndDisparadoFalse(Long userId);
}