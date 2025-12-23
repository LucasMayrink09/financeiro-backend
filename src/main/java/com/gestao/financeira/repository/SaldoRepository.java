package com.gestao.financeira.repository;


import com.gestao.financeira.entity.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {
    List<Saldo> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
