package com.gestao.financeira.repository;


import com.gestao.financeira.entity.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {
}
