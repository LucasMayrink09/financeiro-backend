package com.gestao.financeira.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeConta;
    @Column(precision = 20, scale = 10)
    private BigDecimal valor;
    private String moeda;
    // ===== CRIPTO =====
    private String simbolo; // BTC, ETH, USDT...

    @Column(precision = 20, scale = 10)
    private BigDecimal quantidade;
    private LocalDate data;

    @Column(length = 500)
    private String observacao;

}
