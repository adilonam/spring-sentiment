package ma.code212.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching scraped comments from URLs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.scraping.ttl:3600}")
    private long cacheTtlSeconds;

    @Value("${cache.scraping.key-prefix:scraping}")
    private String keyPrefix;

    /**
     * Cache scraped comments for a URL
     */
    public void cacheScrapedComments(String url, List<String> comments, int totalComments) {
        try {
            String cacheKey = generateCacheKey(url);
            
            ScrapedCommentsCache cacheData = ScrapedCommentsCache.builder()
                    .url(url)
                    .comments(comments)
                    .totalComments(totalComments)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            redisTemplate.opsForValue().set(cacheKey, cacheData, Duration.ofSeconds(cacheTtlSeconds));
            
            log.info("Cached scraped comments for URL: {} with {} comments, TTL: {} seconds", 
                    url, comments.size(), cacheTtlSeconds);
            
        } catch (Exception e) {
            log.error("Failed to cache scraped comments for URL: {}, Error: {}", url, e.getMessage(), e);
        }
    }

    /**
     * Get cached scraped comments for a URL
     */
    public ScrapedCommentsCache getCachedComments(String url) {
        try {
            String cacheKey = generateCacheKey(url);
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData != null) {
                ScrapedCommentsCache result = objectMapper.convertValue(cachedData, ScrapedCommentsCache.class);
                log.info("Cache hit for URL: {} with {} comments", url, result.getComments().size());
                return result;
            } else {
                log.info("Cache miss for URL: {}", url);
                return null;
            }
            
        } catch (Exception e) {
            log.error("Failed to get cached comments for URL: {}, Error: {}", url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if comments are cached for a URL
     */
    public boolean isCached(String url) {
        try {
            String cacheKey = generateCacheKey(url);
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("Failed to check cache for URL: {}, Error: {}", url, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clear cache for a specific URL
     */
    public void clearCache(String url) {
        try {
            String cacheKey = generateCacheKey(url);
            redisTemplate.delete(cacheKey);
            log.info("Cleared cache for URL: {}", url);
        } catch (Exception e) {
            log.error("Failed to clear cache for URL: {}, Error: {}", url, e.getMessage(), e);
        }
    }

    /**
     * Get remaining TTL for cached URL
     */
    public long getRemainingTtl(String url) {
        try {
            String cacheKey = generateCacheKey(url);
            return redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get TTL for URL: {}, Error: {}", url, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Generate cache key for URL using SHA-256 hash
     */
    private String generateCacheKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return keyPrefix + ":" + hexString.toString();
        } catch (Exception e) {
            log.error("Failed to generate cache key for URL: {}, Error: {}", url, e.getMessage(), e);
            // Fallback to base64 encoding
            return keyPrefix + ":" + java.util.Base64.getEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Inner class to represent cached scraped comments data
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScrapedCommentsCache {
        private String url;
        private List<String> comments;
        private int totalComments;
        private long timestamp;
    }
}
