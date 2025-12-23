package com.gestao.financeira.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Senha deve conter: mínimo 8 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}