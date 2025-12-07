package com.gestao.financeira.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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