package com.ohc.groupclass.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.coach.entity.CoachProfile;
import com.ohc.groupclass.coach.mapper.CoachProfileMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.dto.ClassCreateRequest;
import com.ohc.groupclass.course.dto.ClassSessionVO;
import com.ohc.groupclass.course.dto.ClassUpdateRequest;
import com.ohc.groupclass.course.dto.ClassVO;
import com.ohc.groupclass.course.dto.SessionAdjustRequest;
import com.ohc.groupclass.course.dto.SessionItem;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.entity.CourseTemplate;
import com.ohc.groupclass.course.entity.Venue;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import com.ohc.groupclass.course.mapper.CourseTemplateMapper;
import com.ohc.groupclass.course.mapper.VenueMapper;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private static final String ROLE_COACH = "COACH";
    private static final String SESSION_NORMAL = "NORMAL";
    private static final String SESSION_CANCELLED = "CANCELLED";
    private static final Set<String> OCCUPYING_STATUS = Set.of("DRAFT", "OPEN", "CLOSED");
    private static final DateTimeFormatter CLASS_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ClassInfoMapper classInfoMapper;
    private final ClassSessionMapper classSessionMapper;
    private final CourseTemplateMapper courseTemplateMapper;
    private final VenueMapper venueMapper;
    private final CoachProfileMapper coachProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassStatusService classStatusService;

    public List<ClassVO> list(String status, String keyword) {
        LambdaQueryWrapper<ClassInfo> query = Wrappers.lambdaQuery();
        if (status != null && !status.isBlank()) {
            ClassStatus.of(status);
            query.eq(ClassInfo::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(ClassInfo::getTitle, keyword)
                    .or()
                    .like(ClassInfo::getClassNo, keyword));
        }
        query.orderByDesc(ClassInfo::getId);
        return toVOs(classInfoMapper.selectList(query), false);
    }

    public ClassVO getById(Long id) {
        ClassInfo entity = requireClass(id);
        return toVOs(List.of(entity), true).get(0);
    }

    @Transactional
    public ClassVO create(ClassCreateRequest request) {
        CourseTemplate template = requireTemplate(request.getTemplateId());
        CoachProfile coach = requireCoach(request.getCoachId());
        requireVenue(request.getVenueId());
        validateEnrollWindow(request.getEnrollOpenTime(), request.getEnrollCloseTime());

        List<ResolvedSession> sessions = resolveSessions(
                request.getSessions(), request.getVenueId(), request.getCoachId());
        checkScheduleConflict(null, null, sessions);

        ClassInfo entity = new ClassInfo();
        entity.setTemplateId(template.getId());
        entity.setTitle(buildTitle(template, coach));
        entity.setCoachId(request.getCoachId());
        entity.setVenueId(request.getVenueId());
        fillSchedule(entity, sessions);
        entity.setTotalSessions(sessions.size());
        entity.setCapacity(request.getCapacity());
        entity.setPrice(request.getPrice());
        entity.setCoachUnitPrice(request.getCoachUnitPrice() == null
                ? coach.getUnitPrice() : request.getCoachUnitPrice());
        entity.setEnrollOpenTime(request.getEnrollOpenTime());
        entity.setEnrollCloseTime(request.getEnrollCloseTime());
        entity.setStatus(ClassStatus.DRAFT.name());
        entity.setRemark(request.getRemark());
        insertClass(entity);

        insertSessions(entity.getId(), sessions, request.getCoachId());
        return getById(entity.getId());
    }

    @Transactional
    public ClassVO update(Long id, ClassUpdateRequest request) {
        ClassInfo entity = requireClass(id);
        ClassStatus current = ClassStatus.of(entity.getStatus());
        if (current != ClassStatus.DRAFT && current != ClassStatus.OPEN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有草稿或报名中的班级可以修改");
        }
        CoachProfile coach = requireCoach(request.getCoachId());
        requireVenue(request.getVenueId());
        validateEnrollWindow(request.getEnrollOpenTime(), request.getEnrollCloseTime());

        if (current == ClassStatus.OPEN) {
            if (request.getPrice().compareTo(entity.getPrice()) != 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "报名中的班级不能修改价格");
            }
            if (!Objects.equals(request.getCapacity(), entity.getCapacity())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "报名中的班级不能修改名额");
            }
        }

        List<ResolvedSession> sessions = resolveSessions(
                request.getSessions(), request.getVenueId(), request.getCoachId());
        checkScheduleConflict(id, null, sessions);

        List<ClassSession> existing = listSessions(id, true);
        if (current == ClassStatus.OPEN && existing.size() != sessions.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名中的班级不能增减课次");
        }

        CourseTemplate template = requireTemplate(entity.getTemplateId());
        entity.setTitle(buildTitle(template, coach));
        entity.setCoachId(request.getCoachId());
        entity.setVenueId(request.getVenueId());
        fillSchedule(entity, sessions);
        if (current == ClassStatus.DRAFT) {
            entity.setTotalSessions(sessions.size());
        }
        entity.setCapacity(request.getCapacity());
        entity.setPrice(request.getPrice());
        entity.setCoachUnitPrice(request.getCoachUnitPrice() == null
                ? coach.getUnitPrice() : request.getCoachUnitPrice());
        entity.setEnrollOpenTime(request.getEnrollOpenTime());
        entity.setEnrollCloseTime(request.getEnrollCloseTime());
        entity.setRemark(request.getRemark());
        classInfoMapper.updateById(entity);

        if (current == ClassStatus.DRAFT) {
            deleteSessions(id);
            insertSessions(id, sessions, request.getCoachId());
        } else {
            updateSessions(existing, sessions, request.getCoachId());
        }
        return getById(id);
    }

    @Transactional
    public ClassVO publish(Long id) {
        ClassInfo entity = requireClass(id);
        ClassStatus current = ClassStatus.of(entity.getStatus());
        if (current != ClassStatus.DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有草稿班级可以发布");
        }
        if (listSessions(id, true).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "班级还没有课次，不能发布");
        }
        if (entity.getEnrollCloseTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名截止时间已过，请先调整报名时间");
        }
        classStatusService.transition(id, ClassStatus.OPEN);
        return getById(id);
    }

    @Transactional
    public ClassVO cancel(Long id) {
        requireClass(id);
        classStatusService.transition(id, ClassStatus.CANCELLED);
        return getById(id);
    }

    @Transactional
    public ClassVO adjustSession(Long classId, Long sessionId, SessionAdjustRequest request) {
        ClassInfo entity = requireClass(classId);
        ClassStatus status = ClassStatus.of(entity.getStatus());
        if (status == ClassStatus.FINISHED || status == ClassStatus.CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已结束或已取消的班级不能调课");
        }

        ClassSession session = requireSession(classId, sessionId);
        if (!SESSION_NORMAL.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有正常课次可以调课");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已开始的课次不能调课");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课次结束时间必须晚于开始时间");
        }
        if (!LocalDateTime.of(request.getDate(), request.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调课后的上课时间必须晚于当前时间");
        }
        boolean sameDay = listSessions(classId, true).stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sessionId)
                        && other.getStartTime().toLocalDate().equals(request.getDate()));
        if (sameDay) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "同一天只能安排一节课：" + request.getDate());
        }

        Long venueId = request.getVenueId() == null ? session.getVenueId() : request.getVenueId();
        Long coachId = request.getCoachId() == null
                ? (session.getCoachId() == null ? entity.getCoachId() : session.getCoachId())
                : request.getCoachId();
        requireVenue(venueId);
        requireCoach(coachId);

        ResolvedSession resolved = new ResolvedSession(
                request.getDate(), request.getStartTime(), request.getEndTime(), venueId, coachId);
        checkScheduleConflict(null, sessionId, List.of(resolved));

        session.setStartTime(resolved.startDateTime());
        session.setEndTime(resolved.endDateTime());
        session.setVenueId(venueId);
        session.setCoachId(Objects.equals(coachId, entity.getCoachId()) ? null : coachId);
        classSessionMapper.updateById(session);

        refreshClassSchedule(entity);
        return getById(classId);
    }

    @Transactional
    public ClassVO cancelSession(Long classId, Long sessionId) {
        ClassInfo entity = requireClass(classId);
        ClassStatus status = ClassStatus.of(entity.getStatus());
        if (status == ClassStatus.FINISHED || status == ClassStatus.CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已结束或已取消的班级不能停课");
        }

        ClassSession session = requireSession(classId, sessionId);
        if (!SESSION_NORMAL.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有正常课次可以停课");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已开始的课次不能停课");
        }
        if (listSessions(classId, true).size() <= 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少保留一节正常课次，整班不上请取消班级");
        }

        int affected = classSessionMapper.update(null, Wrappers.<ClassSession>lambdaUpdate()
                .eq(ClassSession::getId, session.getId())
                .eq(ClassSession::getStatus, SESSION_NORMAL)
                .set(ClassSession::getStatus, SESSION_CANCELLED));
        if (affected == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "课次状态已变化，请刷新后重试");
        }

        refreshClassSchedule(entity);
        return getById(classId);
    }

    public ClassInfo requireClass(Long id) {
        ClassInfo entity = classInfoMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        return entity;
    }

    private CourseTemplate requireTemplate(Long templateId) {
        CourseTemplate template = courseTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程模板不存在");
        }
        if (template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课程模板已停用：" + template.getName());
        }
        return template;
    }

    private CoachProfile requireCoach(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !ROLE_COACH.equals(user.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "教练不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "教练账号已停用");
        }
        CoachProfile profile = coachProfileMapper.selectOne(
                Wrappers.<CoachProfile>lambdaQuery().eq(CoachProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "教练档案不存在");
        }
        if (profile.getStatus() == null || profile.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "教练已停用");
        }
        return profile;
    }

    private Venue requireVenue(Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场地不存在");
        }
        if (venue.getStatus() == null || venue.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "场地已停用：" + venue.getName());
        }
        return venue;
    }

    private void validateEnrollWindow(LocalDateTime open, LocalDateTime close) {
        if (!open.isBefore(close)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名截止时间必须晚于开放时间");
        }
    }

    private List<ResolvedSession> resolveSessions(List<SessionItem> items, Long defaultVenueId, Long defaultCoachId) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少安排一次课");
        }
        Set<LocalDate> dates = new HashSet<>();
        List<ResolvedSession> result = new ArrayList<>(items.size());
        for (SessionItem item : items) {
            if (!item.getStartTime().isBefore(item.getEndTime())) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        item.getDate() + " 的课次结束时间必须晚于开始时间");
            }
            if (!dates.add(item.getDate())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "同一天只能安排一节课：" + item.getDate());
            }
            result.add(new ResolvedSession(
                    item.getDate(),
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getVenueId() == null ? defaultVenueId : item.getVenueId(),
                    item.getCoachId() == null ? defaultCoachId : item.getCoachId()));
        }
        result.sort(Comparator.comparing(ResolvedSession::date).thenComparing(ResolvedSession::startTime));

        result.stream().map(ResolvedSession::venueId).distinct().forEach(this::requireVenue);
        result.stream().map(ResolvedSession::coachId).distinct().forEach(this::requireCoach);
        return result;
    }

    private void checkScheduleConflict(Long excludeClassId, Long excludeSessionId,
                                       List<ResolvedSession> sessions) {
        LocalDateTime rangeStart = sessions.stream()
                .map(ResolvedSession::startDateTime).min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime rangeEnd = sessions.stream()
                .map(ResolvedSession::endDateTime).max(LocalDateTime::compareTo).orElseThrow();

        LambdaQueryWrapper<ClassSession> query = Wrappers.<ClassSession>lambdaQuery()
                .eq(ClassSession::getStatus, SESSION_NORMAL)
                .lt(ClassSession::getStartTime, rangeEnd)
                .gt(ClassSession::getEndTime, rangeStart);
        if (excludeClassId != null) {
            query.ne(ClassSession::getClassId, excludeClassId);
        }
        List<ClassSession> candidates = classSessionMapper.selectList(query);
        if (candidates.isEmpty()) {
            return;
        }

        Set<Long> classIds = candidates.stream().map(ClassSession::getClassId).collect(Collectors.toSet());
        Map<Long, ClassInfo> classMap = classInfoMapper.selectBatchIds(classIds).stream()
                .filter(item -> OCCUPYING_STATUS.contains(item.getStatus()))
                .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));

        for (ClassSession candidate : candidates) {
            if (excludeSessionId != null && excludeSessionId.equals(candidate.getId())) {
                continue;
            }
            ClassInfo owner = classMap.get(candidate.getClassId());
            if (owner == null) {
                continue;
            }
            Long coachId = candidate.getCoachId() == null ? owner.getCoachId() : candidate.getCoachId();
            Long venueId = candidate.getVenueId() == null ? owner.getVenueId() : candidate.getVenueId();
            for (ResolvedSession item : sessions) {
                boolean overlap = candidate.getStartTime().isBefore(item.endDateTime())
                        && candidate.getEndTime().isAfter(item.startDateTime());
                if (!overlap) {
                    continue;
                }
                if (Objects.equals(coachId, item.coachId())) {
                    throw new BusinessException(ResultCode.CONFLICT,
                            "排期冲突：教练在 " + formatRange(item) + " 已有班级「" + owner.getTitle() + "」");
                }
                if (Objects.equals(venueId, item.venueId())) {
                    throw new BusinessException(ResultCode.CONFLICT,
                            "排期冲突：场地在 " + formatRange(item) + " 已被班级「" + owner.getTitle() + "」占用");
                }
            }
        }
    }

    private String formatRange(ResolvedSession item) {
        return item.date() + " " + item.startTime() + "-" + item.endTime();
    }

    private String buildTitle(CourseTemplate template, CoachProfile coach) {
        return template.getName() + "·" + coach.getRealName() + "班";
    }

    private void fillSchedule(ClassInfo entity, List<ResolvedSession> sessions) {
        ResolvedSession first = sessions.get(0);
        entity.setStartDate(first.date());
        entity.setEndDate(sessions.get(sessions.size() - 1).date());
        entity.setStartTime(first.startTime());
        entity.setEndTime(first.endTime());
    }

    private void refreshClassSchedule(ClassInfo entity) {
        List<ClassSession> remaining = listSessions(entity.getId(), true);
        if (remaining.isEmpty()) {
            return;
        }
        ClassSession first = remaining.stream()
                .min(Comparator.comparing(ClassSession::getStartTime)).orElseThrow();
        ClassSession last = remaining.stream()
                .max(Comparator.comparing(ClassSession::getStartTime)).orElseThrow();
        entity.setStartDate(first.getStartTime().toLocalDate());
        entity.setEndDate(last.getStartTime().toLocalDate());
        entity.setStartTime(first.getStartTime().toLocalTime());
        entity.setEndTime(first.getEndTime().toLocalTime());
        classInfoMapper.updateById(entity);
    }

    private ClassSession requireSession(Long classId, Long sessionId) {
        ClassSession session = classSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getClassId(), classId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课次不存在");
        }
        return session;
    }

    private void insertClass(ClassInfo entity) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            entity.setClassNo(nextClassNo());
            try {
                classInfoMapper.insert(entity);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 3) {
                    throw new BusinessException(ResultCode.CONFLICT, "班级编号生成失败，请重试");
                }
            }
        }
    }

    private String nextClassNo() {
        return "CL" + LocalDateTime.now().format(CLASS_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private void insertSessions(Long classId, List<ResolvedSession> sessions, Long defaultCoachId) {
        int sessionNo = 1;
        for (ResolvedSession item : sessions) {
            ClassSession entity = new ClassSession();
            entity.setClassId(classId);
            entity.setSessionNo(sessionNo++);
            entity.setStartTime(item.startDateTime());
            entity.setEndTime(item.endDateTime());
            entity.setVenueId(item.venueId());
            entity.setCoachId(Objects.equals(item.coachId(), defaultCoachId) ? null : item.coachId());
            entity.setStatus(SESSION_NORMAL);
            classSessionMapper.insert(entity);
        }
    }

    private void updateSessions(List<ClassSession> existing, List<ResolvedSession> sessions, Long defaultCoachId) {
        for (int index = 0; index < existing.size(); index++) {
            ClassSession entity = existing.get(index);
            ResolvedSession item = sessions.get(index);
            entity.setStartTime(item.startDateTime());
            entity.setEndTime(item.endDateTime());
            entity.setVenueId(item.venueId());
            entity.setCoachId(Objects.equals(item.coachId(), defaultCoachId) ? null : item.coachId());
            classSessionMapper.updateById(entity);
        }
    }

    private void deleteSessions(Long classId) {
        classSessionMapper.delete(Wrappers.<ClassSession>lambdaQuery()
                .eq(ClassSession::getClassId, classId));
    }

    private List<ClassSession> listSessions(Long classId, boolean excludeCancelled) {
        LambdaQueryWrapper<ClassSession> query = Wrappers.<ClassSession>lambdaQuery()
                .eq(ClassSession::getClassId, classId)
                .orderByAsc(ClassSession::getSessionNo);
        if (excludeCancelled) {
            query.ne(ClassSession::getStatus, SESSION_CANCELLED);
        }
        return classSessionMapper.selectList(query);
    }

    private List<ClassVO> toVOs(List<ClassInfo> classes, boolean withSessions) {
        if (classes.isEmpty()) {
            return List.of();
        }
        List<Long> classIds = classes.stream().map(ClassInfo::getId).toList();
        Map<Long, List<ClassSession>> sessionMap = withSessions
                ? classSessionMapper.selectList(Wrappers.<ClassSession>lambdaQuery()
                        .in(ClassSession::getClassId, classIds)
                        .orderByAsc(ClassSession::getSessionNo))
                        .stream().collect(Collectors.groupingBy(ClassSession::getClassId))
                : Collections.emptyMap();

        Set<Long> coachIds = new HashSet<>();
        Set<Long> venueIds = new HashSet<>();
        for (ClassInfo item : classes) {
            coachIds.add(item.getCoachId());
            venueIds.add(item.getVenueId());
        }
        sessionMap.values().forEach(sessions -> sessions.forEach(session -> {
            if (session.getCoachId() != null) {
                coachIds.add(session.getCoachId());
            }
            venueIds.add(session.getVenueId());
        }));

        Map<Long, String> coachNames = loadCoachNames(coachIds);
        Map<Long, String> venueNames = loadVenueNames(venueIds);

        List<ClassVO> result = new ArrayList<>(classes.size());
        for (ClassInfo item : classes) {
            List<ClassSessionVO> sessionVOs = null;
            if (withSessions) {
                sessionVOs = sessionMap.getOrDefault(item.getId(), List.of()).stream()
                        .map(session -> {
                            Long coachId = session.getCoachId() == null
                                    ? item.getCoachId() : session.getCoachId();
                            return new ClassSessionVO(
                                    session.getId(),
                                    session.getSessionNo(),
                                    session.getStartTime(),
                                    session.getEndTime(),
                                    session.getVenueId(),
                                    venueNames.get(session.getVenueId()),
                                    coachId,
                                    coachNames.get(coachId),
                                    session.getStatus());
                        })
                        .toList();
            }
            result.add(new ClassVO(
                    item.getId(),
                    item.getClassNo(),
                    item.getTemplateId(),
                    item.getTitle(),
                    item.getCoachId(),
                    coachNames.get(item.getCoachId()),
                    item.getVenueId(),
                    venueNames.get(item.getVenueId()),
                    item.getStartDate(),
                    item.getEndDate(),
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getTotalSessions(),
                    item.getCapacity(),
                    item.getPrice(),
                    item.getCoachUnitPrice(),
                    item.getEnrollOpenTime(),
                    item.getEnrollCloseTime(),
                    item.getStatus(),
                    item.getRemark(),
                    sessionVOs));
        }
        return result;
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

    private record ResolvedSession(LocalDate date, LocalTime startTime, LocalTime endTime,
                                   Long venueId, Long coachId) {

        LocalDateTime startDateTime() {
            return LocalDateTime.of(date, startTime);
        }

        LocalDateTime endDateTime() {
            return LocalDateTime.of(date, endTime);
        }
    }
}
