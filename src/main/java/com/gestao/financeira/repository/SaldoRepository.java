package com.gestao.financeira.repository;


import com.gestao.financeira.entity.Saldo;
import com.gestao.financeira.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {
    boolean existsByUserId(Long userId);
    Page<Saldo> findByUser(User user, Pageable pageable);
}
