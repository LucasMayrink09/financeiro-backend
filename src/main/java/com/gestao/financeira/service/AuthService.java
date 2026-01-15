package com.gestao.financeira.service;

import com.gestao.financeira.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import com.gestao.financeira.dto.*;
import com.gestao.financeira.entity.User;
import com.gestao.financeira.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;
    private final UserService userService;
    private final SocialService socialService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RateLimitService rateLimitService,
                       UserService userService,
                       SocialService socialService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
        this.userService = userService;
        this.socialService = socialService;
    }

    @Transactional
    public LoginResponseDTO login(LoginDTO dto) {
        applyLoginRateLimit(dto.email());
        User user = loadUserByEmail(dto.email());
        validateLoginPreConditions(user);
        validatePassword(dto.password(), user);
        onSuccessfulLogin(user);
        return generateLoginResponse(user);
    }

    @Transactional
    public LoginResponseDTO socialLogin(SocialLoginDTO dto) {
        rateLimitService.consume("social-login-attempt", 10, 60);

        SocialUserInfo info = socialService.validateToken(dto);
        User user = userRepository.findByEmail(info.email().toLowerCase().trim())
                .orElseGet(() -> createSocialUser(info));

        if (!user.isEnabled()) {
            throw new DisabledException("Conta desativada.");
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        return generateLoginResponse(user);
    }

    private User createSocialUser(SocialUserInfo info) {
        User user = new User();
        user.setName(info.name() != null ? info.name() : "Usuário " + info.provider());
        user.setEmail(info.email().toLowerCase().trim());
        String dummyPassword = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        user.setPasswordHash(passwordEncoder.encode(dummyPassword));
        user.setEmailVerified(true);
        user.setLastPasswordChange(Instant.now());
        user.addToPasswordHistory(user.getPasswordHash());
        return userRepository.save(user);
    }

    public String register(UserRegistrationDTO dto) {
        rateLimitService.consume("register:" + dto.email(), 3, 3600);
        validateEmailAvailability(dto.email());
        User user = createUser(dto);
        String token = generateEmailVerification(user);
        userRepository.save(user);
        return token;
    }

    @Transactional
    public User confirmEmail(String token) {
        User user = loadUserByEmailVerificationToken(token);
        validateEmailVerificationToken(token, user);
        confirmUserEmail(user);
        return user;
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        User user = loadUserByPasswordResetToken(dto.token());
        validatePasswordResetToken(dto.token(), user);
        userService.validatePasswordReuse(user, dto.newPassword());
        applyNewPassword(dto.newPassword(), user);
    }

    public String forgotPassword(String email) {
        applyForgotPasswordRateLimit(email);
        User user = findUserByEmailSilently(email);

        if (user == null) {
            return null;
        }
        String code = generatePasswordResetToken(user);
        return code;
    }

    private void applyLoginRateLimit(String email) {
        rateLimitService.consume("login:" + email, 5, 3600);
    }

    private User loadUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Email não cadastrado."));
    }

    private void validateLoginPreConditions(User user) {
        if (!user.isEnabled()) {
            throw new DisabledException("Sua conta foi desativada. Entre em contato com o suporte.");
        }

        if (user.isAccountLocked()) {
            throw new LockedException("Conta temporariamente bloqueada. Tente novamente em alguns minutos.");
        }

        if (!user.isEmailVerified()) {
            throw new DisabledException("Você precisa confirmar seu email antes de fazer login.");
        }
    }

    private void validatePassword(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new BadCredentialsException("Senha incorreta.");
        }
    }

    private void onSuccessfulLogin(User user) {
        user.resetFailedAttempts();
        userRepository.save(user);
    }

    private LoginResponseDTO generateLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return new LoginResponseDTO(token, 3600L);
    }

    private User createUser(UserRegistrationDTO dto) {
        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setEmailVerified(false);
        user.setLastPasswordChange(Instant.now());
        user.addToPasswordHistory(user.getPasswordHash());
        return user;
    }

    private void validateEmailAvailability(String email) {
        if (userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new RegraDeNegocioException("Email já cadastrado");
        }
    }

    private String generateEmailVerification(User user) {
        user.clearEmailVerificationToken();
        String token = UUID.randomUUID().toString();

        user.setEmailVerificationTokenHash(passwordEncoder.encode(token));
        user.setEmailVerificationTokenLookup(DigestUtils.sha256Hex(token));
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(3600));

        return token;
    }

    private User loadUserByEmailVerificationToken(String token) {
        String lookup = DigestUtils.sha256Hex(token);

        return userRepository.findByEmailVerificationTokenLookup(lookup)
                .orElseThrow(() -> new RegraDeNegocioException("Token inválido ou expirado"));
    }

    private void validateEmailVerificationToken(String token, User user) {
        if (!passwordEncoder.matches(token, user.getEmailVerificationTokenHash())) {
            throw new RegraDeNegocioException("Token inválido");
        }

        if (!user.isEmailVerificationTokenValid(token)) {
            throw new IllegalArgumentException("Token expirado");
        }
    }

    private void confirmUserEmail(User user) {
        user.setEmailVerified(true);
        user.clearEmailVerificationToken();
        user.setEmailVerificationTokenLookup(null);
        userRepository.save(user);
    }

    private User loadUserByPasswordResetToken(String token) {
        String lookup = DigestUtils.sha256Hex(token);

        return userRepository.findByPasswordResetTokenLookup(lookup)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));
    }

    private void validatePasswordResetToken(String token, User user) {
        if (!passwordEncoder.matches(token, user.getPasswordResetToken())) {
            throw new IllegalArgumentException("Token inválido");
        }

        if (!user.isPasswordResetTokenValid(token)) {
            throw new IllegalArgumentException("Token expirado");
        }
    }

    private void applyNewPassword(String newPassword, User user) {
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        user.addToPasswordHistory(newHash);
        user.setLastPasswordChange(Instant.now());
        user.clearPasswordResetToken();
        user.setPasswordResetTokenLookup(null);
        userRepository.save(user);
    }

    private void applyForgotPasswordRateLimit(String email) {
        rateLimitService.consume("forgot:" + email, 3, 3600);
    }

    private User findUserByEmailSilently(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private String generatePasswordResetToken(User user) {
        user.clearPasswordResetToken();

        SecureRandom random = new SecureRandom();
        String code = String.format("%06d", random.nextInt(1000000));
        user.setPasswordResetToken(passwordEncoder.encode(code));
        user.setPasswordResetTokenLookup(DigestUtils.sha256Hex(code));
        user.setPasswordResetExpiry(Instant.now().plusSeconds(3600));

        userRepository.save(user);
        return code;
    }
}
