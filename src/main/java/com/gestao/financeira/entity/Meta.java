package com.gestao.financeira.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Meta {

    @Id
    private Long id = 1L;

    private Double valor;
    private String moeda;

}