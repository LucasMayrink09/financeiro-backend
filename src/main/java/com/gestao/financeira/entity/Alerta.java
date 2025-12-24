package com.gestao.financeira.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "alertas", indexes = {
        @Index(name = "idx_alerta_pendente", columnList = "disparado")
})
@Getter @Setter @NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private Long precoAlvo; // Inteiro

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(nullable = false, length = 10)
    private String condicao; // MAIOR ou MENOR

    @Column(nullable = false)
    private boolean disparado = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}