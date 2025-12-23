package com.gestao.financeira.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    record BucketWrapper(Bucket bucket, Instant lastAccess) {}

    private final Map<String, BucketWrapper> buckets = new ConcurrentHashMap<>();

    public void consume(String key, int capacity, int refillSeconds) {
        Bucket bucket = resolveBucket(key, capacity, refillSeconds);

        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente mais tarde."
            );
        }
    }

    public Bucket resolveBucket(String key, int capacity, int refillSeconds) {
        BucketWrapper wrapper = buckets.compute(key, (k, currentWrapper) -> {
            if (currentWrapper == null) {
                Bucket newBucket = Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                capacity,
                                Refill.intervally(capacity, Duration.ofSeconds(refillSeconds))
                        ))
                        .build();
                return new BucketWrapper(newBucket, Instant.now());
            } else {
                return new BucketWrapper(currentWrapper.bucket(), Instant.now());
            }
        });

        return wrapper.bucket();
    }


    @Scheduled(fixedRate = 2400000)
    private void cleanUpExpiredBuckets() {
        int initialSize = buckets.size();

        buckets.entrySet().removeIf(entry ->
                entry.getValue().lastAccess().isBefore(Instant.now().minusSeconds(1800))
        );

        if (buckets.size() > 10000) {
            log.warn("ALERTA DE MEMÓRIA: Rate Limit excedeu 10k entradas. Resetando tudo para evitar crash.");
            buckets.clear();
        } else if (initialSize > 0 && buckets.size() < initialSize) {
            log.info("Limpeza de Rate Limit: {} buckets removidos.", initialSize - buckets.size());
        }
    }
}