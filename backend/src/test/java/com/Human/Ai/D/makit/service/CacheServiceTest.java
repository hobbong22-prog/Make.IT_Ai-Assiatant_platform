package com.Human.Ai.D.makit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache cache;

    @Mock
    private Cache.ValueWrapper valueWrapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testPutWithDefaultTTL() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.put(cacheName, key, value);

        // Then
        verify(cache).put(key, value);
    }

    @Test
    void testPutWithCustomTTL() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";
        Duration ttl = Duration.ofMinutes(10);

        // When
        cacheService.put(cacheName, key, value, ttl);

        // Then
        verify(valueOperations).set(cacheName + "::" + key, value, ttl.toSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void testGetExistingValue() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String expectedValue = "testValue";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(expectedValue);

        // When
        String result = cacheService.get(cacheName, key, String.class);

        // Then
        assertEquals(expectedValue, result);
    }

    @Test
    void testGetNonExistingValue() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key)).thenReturn(null);

        // When
        String result = cacheService.get(cacheName, key, String.class);

        // Then
        assertNull(result);
    }

    @Test
    void testGetWithWrongType() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        Integer wrongTypeValue = 123;
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(wrongTypeValue);

        // When
        String result = cacheService.get(cacheName, key, String.class);

        // Then
        assertNull(result);
    }

    @Test
    void testEvict() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.evict(cacheName, key);

        // Then
        verify(cache).evict(key);
    }

    @Test
    void testClear() {
        // Given
        String cacheName = "testCache";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.clear(cacheName);

        // Then
        verify(cache).clear();
    }

    @Test
    void testExists() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key)).thenReturn(valueWrapper);

        // When
        boolean exists = cacheService.exists(cacheName, key);

        // Then
        assertTrue(exists);
    }

    @Test
    void testNotExists() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key)).thenReturn(null);

        // When
        boolean exists = cacheService.exists(cacheName, key);

        // Then
        assertFalse(exists);
    }

    @Test
    void testGetKeys() {
        // Given
        String pattern = "test*";
        Set<String> expectedKeys = Set.of("test1", "test2", "test3");
        
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        // When
        Set<String> result = cacheService.getKeys(pattern);

        // Then
        assertEquals(expectedKeys, result);
    }

    @Test
    void testInvalidateByPattern() {
        // Given
        String pattern = "test*";
        Set<String> keys = Set.of("test1", "test2", "test3");
        
        when(redisTemplate.keys(pattern)).thenReturn(keys);

        // When
        cacheService.invalidateByPattern(pattern);

        // Then
        verify(redisTemplate).delete(keys);
    }

    @Test
    void testGetCacheStats() {
        // Given
        String cacheName = "testCache";
        Set<String> keys = Set.of("key1", "key2", "key3");
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(redisTemplate.keys(cacheName + "::*")).thenReturn(keys);

        // When
        CacheService.CacheStats stats = cacheService.getCacheStats(cacheName);

        // Then
        assertEquals(cacheName, stats.getCacheName());
        assertEquals(3, stats.getEntryCount());
    }

    @Test
    void testPutHandlesException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";
        
        when(cacheManager.getCache(cacheName)).thenThrow(new RuntimeException("Cache error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> cacheService.put(cacheName, key, value));
    }

    @Test
    void testGetHandlesException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenThrow(new RuntimeException("Cache error"));

        // When
        String result = cacheService.get(cacheName, key, String.class);

        // Then
        assertNull(result);
    }

    @Test
    void testEvictHandlesException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenThrow(new RuntimeException("Cache error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> cacheService.evict(cacheName, key));
    }
}