package com.gestao.financeira.externalservice;

import lombok.extern.slf4j.Slf4j;
import java.util.function.Supplier;

@Slf4j
public class RetryHelper {
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, String operationName) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;

                if (attempt >= maxRetries) {
                    log.error("Falha definitiva em '{}' após {} tentativas", operationName, maxRetries);
                    throw e;
                }

                waitBeforeRetry(attempt, operationName, e);
            }
        }

        throw new RuntimeException("Não deveria chegar aqui");
    }

    private static void waitBeforeRetry(int attempt, String operationName, Exception error) {
        long waitTimeMs = calculateBackoffTime(attempt);

        log.warn("Tentativa {}/3 falhou em '{}'. Aguardando {}ms. Erro: {}",
                attempt, operationName, waitTimeMs, error.getMessage());

        try {
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante retry", ie);
        }
    }

    private static long calculateBackoffTime(int attempt) {
        return (long) Math.pow(2, attempt) * 1000;
    }
}
