package com.ohc.groupclass.enrollment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    public static final int CODE_DUPLICATE = -1;
    public static final int CODE_NOT_READY = -2;
    public static final int CODE_SOLD_OUT = -3;

    private static final String STOCK_KEY_PREFIX = "class:stock:";
    private static final String ENROLLED_KEY_PREFIX = "class:enrolled:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ClassInfoMapper classInfoMapper;
    private final EnrollmentMapper enrollmentMapper;

    private final DefaultRedisScript<Long> enrollScript = script("lua/enroll.lua");
    private final DefaultRedisScript<Long> restoreScript = script("lua/restore.lua");

    public String stockKey(Long classId) {
        return STOCK_KEY_PREFIX + classId;
    }

    public String enrolledKey(Long classId) {
        return ENROLLED_KEY_PREFIX + classId;
    }

    public StockResult tryEnroll(Long classId, Long memberId) {
        Long code = stringRedisTemplate.execute(
                enrollScript,
                List.of(stockKey(classId), enrolledKey(classId)),
                memberId.toString());
        if (code == null) {
            return new StockResult(CODE_NOT_READY, null);
        }
        if (code >= 0) {
            return new StockResult(1, code.intValue());
        }
        return new StockResult(code.intValue(), null);
    }

    /**
     * 报名落库成功后名额已归还数据库，只有在 Redis key 仍存在时才回补，
     * key 不存在说明缓存已丢，下一次重建会按数据库重新算，不能盲目 INCR。
     */
    public void release(Long classId, Long memberId) {
        try {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey(classId)))) {
                log.warn("[库存] {} 不存在，取消后跳过回补，等待重建", stockKey(classId));
                return;
            }
            stringRedisTemplate.opsForValue().increment(stockKey(classId));
            stringRedisTemplate.opsForSet().remove(enrolledKey(classId), memberId.toString());
        } catch (Exception e) {
            log.warn("[库存] 回补失败 classId={} memberId={}：{}", classId, memberId, e.getMessage());
        }
    }

    /**
     * 落库失败后的补偿：把刚扣掉的名额加回去。
     * key 不存在时不做处理，交给重建兜底，避免凭空空写出一个库存。
     */
    public void compensate(Long classId, Long memberId) {
        try {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey(classId)))) {
                return;
            }
            stringRedisTemplate.opsForValue().increment(stockKey(classId));
            stringRedisTemplate.opsForSet().remove(enrolledKey(classId), memberId.toString());
        } catch (Exception e) {
            log.warn("[库存] 补偿失败 classId={} memberId={}：{}", classId, memberId, e.getMessage());
        }
    }

    public Integer remaining(Long classId) {
        String value = stringRedisTemplate.opsForValue().get(stockKey(classId));
        return value == null ? null : Integer.valueOf(value);
    }

    public Map<Long, Integer> remainingBatch(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        List<String> keys = classIds.stream().map(this::stockKey).toList();
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        Map<Long, Integer> result = new HashMap<>();
        if (values == null) {
            return result;
        }
        for (int i = 0; i < classIds.size(); i++) {
            String value = values.get(i);
            if (value != null) {
                result.put(classIds.get(i), Integer.valueOf(value));
            }
        }
        return result;
    }

    /**
     * 以数据库为准重建单个班级的库存与占位集合，整段用 Lua 原子覆盖。
     */
    public int rebuild(Long classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        return rebuild(classInfo);
    }

    public int rebuildAll() {
        List<ClassInfo> classes = classInfoMapper.selectList(Wrappers.<ClassInfo>lambdaQuery()
                .in(ClassInfo::getStatus, ClassStatus.OPEN.name(), ClassStatus.CLOSED.name()));
        for (ClassInfo classInfo : classes) {
            rebuild(classInfo);
        }
        return classes.size();
    }

    private int rebuild(ClassInfo classInfo) {
        long occupied = enrollmentMapper.countOccupied(classInfo.getId());
        int remaining = (int) Math.max(0, classInfo.getCapacity() - occupied);
        List<Long> memberIds = enrollmentMapper.selectOccupiedMemberIds(classInfo.getId());

        List<String> args = new ArrayList<>(memberIds.size() + 1);
        args.add(String.valueOf(remaining));
        memberIds.forEach(id -> args.add(String.valueOf(id)));

        stringRedisTemplate.execute(
                restoreScript,
                List.of(stockKey(classInfo.getId()), enrolledKey(classInfo.getId())),
                args.toArray());
        return remaining;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    public record StockResult(int code, Integer remaining) {
    }
}
