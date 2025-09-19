package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing cached data with Redis
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Store data in cache with default TTL
     */
    public void put(String cacheName, String key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                logger.debug("Cached data with key: {} in cache: {}", key, cacheName);
            }
        } catch (Exception e) {
            logger.error("Error caching data with key: {} in cache: {}", key, cacheName, e);
        }
    }

    /**
     * Store data in cache with custom TTL
     */
    public void put(String cacheName, String key, Object value, Duration ttl) {
        try {
            String fullKey = cacheName + "::" + key;
            redisTemplate.opsForValue().set(fullKey, value, ttl.toSeconds(), TimeUnit.SECONDS);
            logger.debug("Cached data with key: {} in cache: {} with TTL: {}", key, cacheName, ttl);
        } catch (Exception e) {
            logger.error("Error caching data with key: {} in cache: {} with TTL: {}", key, cacheName, ttl, e);
        }
    }

    /**
     * Retrieve data from cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    if (type.isInstance(value)) {
                        logger.debug("Cache hit for key: {} in cache: {}", key, cacheName);
                        return (T) value;
                    }
                }
            }
            logger.debug("Cache miss for key: {} in cache: {}", key, cacheName);
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving data with key: {} from cache: {}", key, cacheName, e);
            return null;
        }
    }

    /**
     * Remove specific key from cache
     */
    public void evict(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted key: {} from cache: {}", key, cacheName);
            }
        } catch (Exception e) {
            logger.error("Error evicting key: {} from cache: {}", key, cacheName, e);
        }
    }

    /**
     * Clear entire cache
     */
    public void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        } catch (Exception e) {
            logger.error("Error clearing cache: {}", cacheName, e);
        }
    }

    /**
     * Check if key exists in cache
     */
    public boolean exists(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                return cache.get(key) != null;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking existence of key: {} in cache: {}", key, cacheName, e);
            return false;
        }
    }

    /**
     * Get all keys matching pattern
     */
    public Set<String> getKeys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            logger.error("Error getting keys with pattern: {}", pattern, e);
            return Set.of();
        }
    }

    /**
     * Invalidate cache entries by pattern
     */
    public void invalidateByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Invalidated {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            logger.error("Error invalidating keys with pattern: {}", pattern, e);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Basic stats - in a real implementation, you might want to use Micrometer
                Set<String> keys = getKeys(cacheName + "::*");
                return new CacheStats(cacheName, keys.size());
            }
            return new CacheStats(cacheName, 0);
        } catch (Exception e) {
            logger.error("Error getting cache stats for: {}", cacheName, e);
            return new CacheStats(cacheName, 0);
        }
    }

    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        private final String cacheName;
        private final long entryCount;

        public CacheStats(String cacheName, long entryCount) {
            this.cacheName = cacheName;
            this.entryCount = entryCount;
        }

        public String getCacheName() {
            return cacheName;
        }

        public long getEntryCount() {
            return entryCount;
        }
    }
}