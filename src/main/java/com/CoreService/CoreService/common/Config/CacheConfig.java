package com.CoreService.CoreService.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Caches hold derived, reconstructible data only: MySQL remains the source of
 * truth.
 * <p>
 * Spring Boot 4 moved cache auto-configuration into a module that is not on
 * this classpath, so {@code spring.cache.type} has no effect here and
 * {@code @EnableCaching} would fail without a {@link CacheManager} bean. This
 * class therefore always provides exactly one, selected by
 * {@code app.cache.provider}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ROLE_PERMISSIONS_CACHE = "rolePermissions";
    public static final String COLLEGE_MODULES_CACHE = "collegeModules";

    @Bean
    @ConditionalOnProperty(name = "app.cache.provider", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration(ROLE_PERMISSIONS_CACHE, defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(COLLEGE_MODULES_CACHE, defaults.entryTtl(Duration.ofMinutes(15)))
                .build();
    }

    /** Used by tests and by any deployment without Redis. */
    @Bean
    @ConditionalOnProperty(name = "app.cache.provider", havingValue = "memory")
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(ROLE_PERMISSIONS_CACHE, COLLEGE_MODULES_CACHE);
    }
}
