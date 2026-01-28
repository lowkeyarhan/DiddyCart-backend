package com.diddycart.common.config;

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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.Callable;

@Configuration
public class CacheConfig {

    // Cache Manager Configuration
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure JSON Serialization
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

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

    // A Decorator for CacheManager that returns LoggingCache instances
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

    // A Decorator for Cache that returns LoggingCache instances
    static class LoggingCache implements Cache {
        private final Cache delegate;
        private final Logger log = LoggerFactory.getLogger(LoggingCache.class);

        // Constructor
        public LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        // Get value from cache
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

        // Get value from cache by key and type
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

        // Get value from cache by key and valueLoader
        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            log.debug("🔍 CACHE LOAD | Cache: {} | Key: {}", getName(), key);
            return delegate.get(key, valueLoader);
        }

        // Put value into cache
        @Override
        public void put(Object key, Object value) {
            log.info("💾 CACHE PUT  | Cache: {} | Key: {}", getName(), key);
            delegate.put(key, value);
        }

        // Evict value from cache
        @Override
        public void evict(Object key) {
            log.info("🗑️ CACHE EVICT| Cache: {} | Key: {}", getName(), key);
            delegate.evict(key);
        }

        // Clear cache
        @Override
        public void clear() {
            log.info("🧹 CACHE CLEAR| Cache: {}", getName());
            delegate.clear();
        }

        // Standard Delegation Methods
        @Override
        public String getName() {
            return delegate.getName();
        }

        // Get native cache
        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        // Put value into cache if absent
        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(key, value);
        }

        // Evict value from cache if present
        @Override
        public boolean evictIfPresent(Object key) {
            return delegate.evictIfPresent(key);
        }

        // Invalidate cache
        @Override
        public boolean invalidate() {
            return delegate.invalidate();
        }
    }
}