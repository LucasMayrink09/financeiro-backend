package com.gestao.financeira.repository;

import com.gestao.financeira.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailVerificationTokenLookup(String lookup);
    Optional<User> findByPasswordResetTokenLookup(String lookup);
}
