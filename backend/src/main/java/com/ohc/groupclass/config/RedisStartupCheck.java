package com.ohc.groupclass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisStartupCheck {

    private static final String KEY = "health:startup";

    @Bean
    public ApplicationRunner redisHealthRunner(StringRedisTemplate stringRedisTemplate) {
        return args -> {
            try {
                stringRedisTemplate.opsForValue().set(KEY, "ok", Duration.ofSeconds(30));
                String readBack = stringRedisTemplate.opsForValue().get(KEY);
                log.info("[Redis] 连通正常，{} = {}", KEY, readBack);
            } catch (Exception e) {
                log.warn("[Redis] 连接失败：{}", e.getMessage());
            }
        };
    }
}