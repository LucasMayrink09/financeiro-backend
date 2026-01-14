package com.gestao.financeira.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Trata erros de validação do DTO (@Valid, @NotNull, @Email)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Pega cada campo que deu erro e a mensagem associada
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 2. Trata erros de Regra de Negócio (Email duplicado, Senha fraca)
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRule(RegraDeNegocioException ex) {
        // Retorna um JSON simples: {"error": "Email já cadastrado"}
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    // 3. Trata qualquer outro erro não previsto (NullPointer, Banco fora do ar)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Erro não tratado: {}", ex.getMessage(), ex); // Log completo
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor. Contate o suporte."));
    }

    @ExceptionHandler({RuntimeException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({BadCredentialsException.class, LockedException.class, DisabledException.class})
    public ResponseEntity<Map<String, String>> handleAuthException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, String>> handleAiException(AiServiceException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }
}