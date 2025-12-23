package com.gestao.financeira.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            return false;
        }

        if (password.length() > 128) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Senha não pode ter mais de 128 caracteres"
            ).addConstraintViolation();
            return false;
        }

        String[] commonPasswords = {
                "Password123!", "Welcome123!", "Admin123!",
                "Qwerty123!", "Password@123", "12345678aA!"
        };

        for (String common : commonPasswords) {
            if (password.equals(common)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Senha muito comum. Escolha uma senha mais segura."
                ).addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
