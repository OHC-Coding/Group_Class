package com.ohc.groupclass.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.coach.entity.CoachProfile;
import com.ohc.groupclass.coach.mapper.CoachProfileMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.dto.MemberClassVO;
import com.ohc.groupclass.course.dto.MemberSessionVO;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.entity.CourseTemplate;
import com.ohc.groupclass.course.entity.Venue;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import com.ohc.groupclass.course.mapper.CourseTemplateMapper;
import com.ohc.groupclass.course.mapper.VenueMapper;
import com.ohc.groupclass.enrollment.dto.StockVO;
import com.ohc.groupclass.enrollment.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberClassService {

    private static final String SESSION_NORMAL = "NORMAL";

    private final ClassInfoMapper classInfoMapper;
    private final ClassSessionMapper classSessionMapper;
    private final CourseTemplateMapper courseTemplateMapper;
    private final VenueMapper venueMapper;
    private final CoachProfileMapper coachProfileMapper;
    private final StockService stockService;

    public List<MemberClassVO> listOpen(String keyword) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ClassInfo> query = Wrappers.<ClassInfo>lambdaQuery()
                .eq(ClassInfo::getStatus, ClassStatus.OPEN.name())
                .le(ClassInfo::getEnrollOpenTime, now)
                .gt(ClassInfo::getEnrollCloseTime, now)
                .ge(ClassInfo::getEndDate, LocalDate.now())
                .orderByAsc(ClassInfo::getEnrollCloseTime)
                .orderByAsc(ClassInfo::getId);
        if (keyword != null && !keyword.isBlank()) {
            query.like(ClassInfo::getTitle, keyword);
        }

        List<ClassInfo> classes = classInfoMapper.selectList(query);
        if (classes.isEmpty()) {
            return List.of();
        }

        List<Long> classIds = classes.stream().map(ClassInfo::getId).toList();
        Map<Long, List<ClassSession>> sessionMap = classSessionMapper.selectList(
                        Wrappers.<ClassSession>lambdaQuery()
                                .in(ClassSession::getClassId, classIds)
                                .eq(ClassSession::getStatus, SESSION_NORMAL)
                                .orderByAsc(ClassSession::getStartTime))
                .stream().collect(Collectors.groupingBy(ClassSession::getClassId));

        Set<Long> templateIds = classes.stream().map(ClassInfo::getTemplateId)
                .collect(Collectors.toSet());
        Map<Long, CourseTemplate> templateMap = courseTemplateMapper.selectBatchIds(templateIds)
                .stream().collect(Collectors.toMap(CourseTemplate::getId, Function.identity()));

        Set<Long> coachIds = new HashSet<>();
        Set<Long> venueIds = new HashSet<>();
        classes.forEach(item -> {
            coachIds.add(item.getCoachId());
            venueIds.add(item.getVenueId());
        });
        sessionMap.values().forEach(sessions -> sessions.forEach(session -> {
            if (session.getCoachId() != null) {
                coachIds.add(session.getCoachId());
            }
            venueIds.add(session.getVenueId());
        }));
        Map<Long, String> coachNames = loadCoachNames(coachIds);
        Map<Long, String> venueNames = loadVenueNames(venueIds);
        Map<Long, Integer> remainingMap = loadRemaining(classIds);

        return classes.stream().map(item -> {
            CourseTemplate template = templateMap.get(item.getTemplateId());
            List<MemberSessionVO> sessions = sessionMap.getOrDefault(item.getId(), List.of())
                    .stream()
                    .map(session -> {
                        Long coachId = session.getCoachId() == null
                                ? item.getCoachId() : session.getCoachId();
                        return new MemberSessionVO(
                                session.getStartTime().toLocalDate(),
                                session.getStartTime().toLocalTime(),
                                session.getEndTime().toLocalTime(),
                                venueNames.get(session.getVenueId()),
                                coachNames.get(coachId));
                    })
                    .toList();
            return new MemberClassVO(
                    item.getId(),
                    item.getTitle(),
                    template == null ? null : template.getSportType(),
                    coachNames.get(item.getCoachId()),
                    venueNames.get(item.getVenueId()),
                    item.getStartDate(),
                    item.getEndDate(),
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getTotalSessions(),
                    item.getCapacity(),
                    remainingMap.get(item.getId()),
                    item.getPrice(),
                    item.getEnrollOpenTime(),
                    item.getEnrollCloseTime(),
                    sessions);
        }).toList();
    }

    public StockVO getStock(Long classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        Integer remaining = stockService.remaining(classId);
        if (remaining == null) {
            remaining = stockService.rebuild(classId);
        }
        return new StockVO(classId, classInfo.getCapacity(),
                Math.max(0, classInfo.getCapacity() - remaining), remaining);
    }

    private Map<Long, Integer> loadRemaining(List<Long> classIds) {
        try {
            Map<Long, Integer> result = new HashMap<>(stockService.remainingBatch(classIds));
            for (Long classId : classIds) {
                if (!result.containsKey(classId)) {
                    result.put(classId, stockService.rebuild(classId));
                }
            }
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<Long, String> loadCoachNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return coachProfileMapper.selectList(Wrappers.<CoachProfile>lambdaQuery()
                        .in(CoachProfile::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(CoachProfile::getUserId, CoachProfile::getRealName, (a, b) -> a));
    }

    private Map<Long, String> loadVenueNames(Collection<Long> venueIds) {
        if (venueIds.isEmpty()) {
            return Map.of();
        }
        return venueMapper.selectBatchIds(venueIds).stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));
    }
}
