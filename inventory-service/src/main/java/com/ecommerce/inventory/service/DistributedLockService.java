package com.ecommerce.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final long DEFAULT_WAIT_TIME = 5;
    private static final long DEFAULT_LEASE_TIME = 10;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 200;

    private final RedissonClient redissonClient;

    public String buildLockKey(String region, UUID productId) {
        return "inventory:lock:" + region + ":" + productId;
    }

    public boolean acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
    }

    public boolean acquireLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
                if (acquired) {
                    log.debug("Lock acquired: {} (attempt {})", lockKey, attempt);
                    return true;
                }
                log.warn("Failed to acquire lock: {} (attempt {}/{})", lockKey, attempt, MAX_RETRIES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while acquiring lock: {}", lockKey, e);
                return false;
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(backoff);
                    backoff *= 2;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    public void releaseLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released: {}", lockKey);
        }
    }
}
