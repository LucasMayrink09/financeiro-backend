package com.gestao.financeira.service;

import com.gestao.financeira.dto.ChangePasswordDTO;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.exception.RegraDeNegocioException;
import com.gestao.financeira.repository.SaldoRepository;
import com.gestao.financeira.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SaldoRepository saldoRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       SaldoRepository saldoRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.saldoRepository = saldoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void banUser(Long userId) {
        User user = findUserByIdOrThrow(userId);
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserByIdOrThrow(userId);

        if (saldoRepository.existsByUserId(userId)) {
            throw new RegraDeNegocioException("ERRO: Usuário possui dados financeiros (Saldos). Use a função BANIR para não perder histórico.");
        }
        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordDTO data) {
        User user = findUserByEmailOrThrow(email);

        validateCurrentPassword(user, data.currentPassword());
        validatePasswordReuse(user, data.newPassword());
        applyNewPassword(user, data.newPassword());

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado com o email: " + email));
    }

    public Long getUserIdByEmail(String email) {
        return findUserByEmailOrThrow(email).getId();
    }

    public User getUserByEmail(String email) {
        return findUserByEmailOrThrow(email);
    }

    private void validateCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }
    }

    @Transactional(readOnly = true)
    public void validatePasswordReuse(User user, String newPassword) {
        for (String oldHash : user.getPasswordHistory()) {
            if (passwordEncoder.matches(newPassword, oldHash)) {
                throw new RegraDeNegocioException("Você não pode reutilizar uma senha recente");
            }
        }
    }

    private void applyNewPassword(User user, String newPassword) {
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        user.addToPasswordHistory(newHash);
        user.setLastPasswordChange(Instant.now());
    }
}