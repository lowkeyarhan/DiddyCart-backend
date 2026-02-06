package com.diddycart.common.infrastructure;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

        private final RedissonBasedProxyManager proxyManager;

        @Autowired
        public RateLimiterService(RedissonClient redissonClient) {
                // Configure Redis backend for Bucket4j
                // Access CommandAsyncExecutor through Redisson implementation class
                org.redisson.Redisson redisson = (org.redisson.Redisson) redissonClient;
                CommandAsyncExecutor commandExecutor = redisson.getCommandExecutor();

                this.proxyManager = RedissonBasedProxyManager.builderFor(commandExecutor)
                                .withExpirationStrategy(ExpirationAfterWriteStrategy
                                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                                .build();
        }

        public Bucket resolveBucket(String key, boolean isAuthenticated) {
                // Define limits based on authentication status
                BucketConfiguration configuration = isAuthenticated
                                ? getAuthenticatedConfig() // 100 req/min for Users
                                : getAnonymousConfig(); // 20 req/min for IPs (Stricter!)

                // Retrieve or create the bucket in Redis
                return proxyManager.builder().build(key, configuration);
        }

        // Limit for Logged-in Users (User ID based)
        private BucketConfiguration getAuthenticatedConfig() {
                return BucketConfiguration.builder()
                                .addLimit(Bandwidth.builder()
                                                .capacity(100)
                                                .refillGreedy(100, Duration.ofMinutes(1))
                                                .build())
                                .build();
        }

        // Limit for Anonymous Users (IP based) - Prevents DDoS
        private BucketConfiguration getAnonymousConfig() {
                return BucketConfiguration.builder()
                                .addLimit(Bandwidth.builder()
                                                .capacity(20) // Strict limit!
                                                .refillGreedy(20, Duration.ofMinutes(1))
                                                .build())
                                .build();
        }
}