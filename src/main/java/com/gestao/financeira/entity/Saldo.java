package com.gestao.financeira.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String nomeConta;

    // IMPORTANTE: Para investimentos variáveis, este passa a ser o VALOR INVESTIDO (Custo)
    @Column(precision = 20, scale = 10)
    private BigDecimal valor;
    // Define qual API o front vai chamar (Brapi, CoinGecko ou Calculadora CDI)
    // Valores sugeridos: "ACAO", "FII", "ETF", "CRIPTO", "RENDA_FIXA"
    private String tipo;
    private String moeda;
    // ===== CRIPTO =====
    private String simbolo; // PETR4, BTC, ou null para Renda Fixa
    @Column(precision = 20, scale = 10)
    private BigDecimal quantidade; // Para Ações/Cripto (número de cotas)
    // Para Renda Fixa. Armazena o percentual (Ex: 110.0 para 110% do CDI)
    @Column(precision = 10, scale = 2)
    private BigDecimal taxa;

    private LocalDate data; // Data da compra (Essencial para calcular CDI acumulado)

    @Column(length = 500)
    private String observacao;

}
