package com.gestao.financeira.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = Set.of("ROLE_USER");

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(length = 64, unique = true)
    private String emailVerificationTokenHash;

    @Column(length = 64, unique = true)
    private String emailVerificationTokenLookup;

    @Column
    private Instant emailVerificationExpiry;

    @Column(length = 64, unique = true)
    private String passwordResetTokenHash;

    @Column(length = 64, unique = true)
    private String passwordResetTokenLookup;

    @Column
    private Instant passwordResetExpiry;

    @Column
    private Instant lockedUntil;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @ElementCollection
    @CollectionTable(name = "user_password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "password_hash", length = 60)
    @OrderColumn(name = "position")
    private List<String> passwordHistory = new ArrayList<>();

    @Column
    private Instant lastPasswordChange;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }


    public String getPasswordResetToken() {
        return passwordResetTokenHash;
    }

    public void setPasswordResetToken(String hash) {
        this.passwordResetTokenHash = hash;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void incrementFailedAttempts() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) {
            lockedUntil = Instant.now().plusSeconds(900); // 15 min
        }
    }

    public void resetFailedAttempts() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public boolean isEmailVerificationTokenValid(String token) {
        return emailVerificationTokenHash != null
                && emailVerificationExpiry != null
                && emailVerificationExpiry.isAfter(Instant.now());
    }

    public boolean isPasswordResetTokenValid(String token) {
        return passwordResetTokenHash != null
                && passwordResetExpiry != null
                && passwordResetExpiry.isAfter(Instant.now());
    }

    public void clearEmailVerificationToken() {
        this.emailVerificationTokenHash = null;
        this.emailVerificationTokenLookup = null;
        this.emailVerificationExpiry = null;
    }

    public void clearPasswordResetToken() {
        this.passwordResetTokenHash = null;
        this.passwordResetTokenLookup = null;
        this.passwordResetExpiry = null;
    }

    public void addToPasswordHistory(String hash) {
        passwordHistory.add(0, hash);
        if (passwordHistory.size() > 5) {
            passwordHistory.remove(passwordHistory.size() - 1);
        }
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE,
        MICROSOFT
    }

}