package com.diddycart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.concurrent.Callable;

@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure ObjectMapper with type validation
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        // Configure JSON Serialization
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                // Use JSON instead of Java ByteStream
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        RedisCacheManager redisManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();

        // Wrap it with our Logging Decorator
        return new LoggingCacheManager(redisManager);
    }

    /**
     * A Decorator for CacheManager that returns LoggingCache instances.
     */
    static class LoggingCacheManager implements CacheManager {
        private final CacheManager delegate;

        public LoggingCacheManager(CacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            return new LoggingCache(cache);
        }

        @Override
        public java.util.Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }

    /**
     * A Decorator for Cache that logs Hits, Misses, Puts, and Evictions.
     */
    static class LoggingCache implements Cache {
        private final Cache delegate;
        private final Logger log = LoggerFactory.getLogger(LoggingCache.class);

        public LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            if (value != null) {
                log.info("✅ CACHE HIT  | Cache: {} | Key: {}", getName(), key);
            } else {
                log.info("❌ CACHE MISS | Cache: {} | Key: {}", getName(), key);
            }
            return value;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            if (value != null) {
                log.info("✅ CACHE HIT  | Cache: {} | Key: {}", getName(), key);
            } else {
                log.info("❌ CACHE MISS | Cache: {} | Key: {}", getName(), key);
            }
            return value;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            log.debug("🔍 CACHE LOAD | Cache: {} | Key: {}", getName(), key);
            return delegate.get(key, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            log.info("💾 CACHE PUT  | Cache: {} | Key: {}", getName(), key);
            delegate.put(key, value);
        }

        @Override
        public void evict(Object key) {
            log.info("🗑️ CACHE EVICT| Cache: {} | Key: {}", getName(), key);
            delegate.evict(key);
        }

        @Override
        public void clear() {
            log.info("🧹 CACHE CLEAR| Cache: {}", getName());
            delegate.clear();
        }

        // --- Standard Delegation Methods ---
        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            return delegate.evictIfPresent(key);
        }

        @Override
        public boolean invalidate() {
            return delegate.invalidate();
        }
    }
}