package com.ohc.groupclass.system;

import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class HealthController {

    private static final String KEY = "health:ping";

    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/redis")
    public Result<Map<String, Object>> redis() {
        String value = String.valueOf(System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(KEY, value, Duration.ofSeconds(60));
        String readBack = stringRedisTemplate.opsForValue().get(KEY);
        if (!value.equals(readBack)) {
            throw new BusinessException(ResultCode.ERROR, "Redis 写入后读回不一致");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        data.put("key", KEY);
        data.put("value", readBack);
        return Result.ok(data);
    }
}