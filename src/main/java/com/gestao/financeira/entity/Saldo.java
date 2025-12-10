package com.gestao.financeira.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeConta;
    private Double valor;
    private String moeda;
    private LocalDate data;

    @Column(length = 500)
    private String observacao;

}
