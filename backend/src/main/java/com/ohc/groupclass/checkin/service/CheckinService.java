package com.ohc.groupclass.checkin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.checkin.dto.CheckinCodeVO;
import com.ohc.groupclass.checkin.dto.CheckinResultVO;
import com.ohc.groupclass.checkin.dto.RosterItemVO;
import com.ohc.groupclass.checkin.dto.RosterVO;
import com.ohc.groupclass.checkin.entity.CheckinRecord;
import com.ohc.groupclass.checkin.mapper.CheckinRecordMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.entity.Venue;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import com.ohc.groupclass.course.mapper.VenueMapper;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.enums.EnrollmentStatus;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final Logger log = LoggerFactory.getLogger(CheckinService.class);

    private static final String SESSION_NORMAL = "NORMAL";
    private static final String SOURCE_CODE = "CODE";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String STATUS_CHECKED = "CHECKED";
    private static final String STATUS_ABSENT = "ABSENT";
    private static final String STATUS_NOT_CHECKED = "NOT_CHECKED";
    private static final int OPEN_BEFORE_MINUTES = 30;
    private static final int CODE_EXTRA_MINUTES = 60;
    private static final int CODE_UPPER_BOUND = 1_000_000;

    private final ClassSessionMapper classSessionMapper;
    private final ClassInfoMapper classInfoMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final VenueMapper venueMapper;

    /**
     * 课次签到码：有效期内复用，过期或没有才重新生成，避免教练多次打开页面把码刷掉。
     */
    public CheckinCodeVO issueCode(Long sessionId, Long coachUserId) {
        ClassSession session = requireSession(sessionId);
        ClassInfo classInfo = requireCoach(session, coachUserId);
        requireNormal(session);

        LocalDateTime now = LocalDateTime.now();
        boolean needNew = session.getCheckinCode() == null
                || session.getCheckinExpireAt() == null
                || !now.isBefore(session.getCheckinExpireAt());
        if (needNew) {
            LocalDateTime expireAt = session.getEndTime().plusMinutes(CODE_EXTRA_MINUTES);
            if (!now.isBefore(expireAt)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "课次已结束，无法生成签到码");
            }
            classSessionMapper.update(null, Wrappers.<ClassSession>lambdaUpdate()
                    .eq(ClassSession::getId, sessionId)
                    .and(wrapper -> wrapper.isNull(ClassSession::getCheckinCode)
                            .or()
                            .isNull(ClassSession::getCheckinExpireAt)
                            .or()
                            .lt(ClassSession::getCheckinExpireAt, now))
                    .set(ClassSession::getCheckinCode, randomCode())
                    .set(ClassSession::getCheckinExpireAt, expireAt));
            session = classSessionMapper.selectById(sessionId);
        }
        return buildCode(session, classInfo);
    }

    public RosterVO roster(Long sessionId, Long coachUserId) {
        ClassSession session = requireSession(sessionId);
        ClassInfo classInfo = requireCoach(session, coachUserId);
        return buildRoster(session, classInfo);
    }

    /**
     * 教练按名单勾选核销，逐个处理；单个学员失败不影响其他人。
     */
    public List<CheckinResultVO> manualCheckin(Long sessionId, List<Long> memberIds, Long coachUserId) {
        ClassSession session = requireSession(sessionId);
        ClassInfo classInfo = requireCoach(session, coachUserId);
        requireNormal(session);
        Long coachId = session.getCoachId() == null ? classInfo.getCoachId() : session.getCoachId();

        List<CheckinResultVO> results = new ArrayList<>();
        Set<Long> distinct = new LinkedHashSet<>(memberIds);
        for (Long memberId : distinct) {
            Enrollment enrollment = findEnrollment(session.getClassId(), memberId);
            if (enrollment == null
                    || !EnrollmentStatus.CONFIRMED.name().equals(enrollment.getStatus())) {
                results.add(new CheckinResultVO(memberId, null, false, false, null, "该学员未确认报名"));
                continue;
            }
            results.add(doCheckin(session, enrollment, coachId, SOURCE_MANUAL));
        }
        return results;
    }

    /**
     * 会员凭课次签到码自助核销：校验码、时间窗、报名状态，重复提交返回已有记录。
     */
    public CheckinResultVO checkinByCode(Long sessionId, Long memberId, String code) {
        ClassSession session = requireSession(sessionId);
        requireNormal(session);
        if (session.getCheckinCode() == null || !session.getCheckinCode().equals(code.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "签到码不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime().minusMinutes(OPEN_BEFORE_MINUTES))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "签到尚未开始");
        }
        LocalDateTime deadline = session.getCheckinExpireAt() == null
                ? session.getEndTime() : session.getCheckinExpireAt();
        if (now.isAfter(deadline)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "签到已截止");
        }

        Enrollment enrollment = findEnrollment(session.getClassId(), memberId);
        if (enrollment == null
                || !EnrollmentStatus.CONFIRMED.name().equals(enrollment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你未报名该班级");
        }
        ClassInfo classInfo = classInfoMapper.selectById(session.getClassId());
        Long coachId = session.getCoachId() == null ? classInfo.getCoachId() : session.getCoachId();
        return doCheckin(session, enrollment, coachId, SOURCE_CODE);
    }

    private CheckinResultVO doCheckin(ClassSession session, Enrollment enrollment,
                                      Long coachId, String source) {
        CheckinRecord existing = findRecord(session.getId(), enrollment.getId());
        if (existing != null) {
            return duplicate(existing);
        }

        CheckinRecord record = new CheckinRecord();
        record.setSessionId(session.getId());
        record.setClassId(session.getClassId());
        record.setEnrollmentId(enrollment.getId());
        record.setMemberId(enrollment.getMemberId());
        record.setCoachId(coachId);
        record.setCheckinTime(LocalDateTime.now());
        record.setSource(source);
        try {
            checkinRecordMapper.insert(record);
            log.info("[核销] 课次 {} 学员 {} 来源 {} 核销成功", session.getId(),
                    enrollment.getMemberId(), source);
            return new CheckinResultVO(enrollment.getMemberId(), enrollment.getId(),
                    true, false, record.getCheckinTime(), "核销成功");
        } catch (DuplicateKeyException e) {
            CheckinRecord latest = findRecord(session.getId(), enrollment.getId());
            if (latest == null) {
                return new CheckinResultVO(enrollment.getMemberId(), enrollment.getId(),
                        false, false, null, "重复提交，请稍后刷新");
            }
            return duplicate(latest);
        }
    }

    private RosterVO buildRoster(ClassSession session, ClassInfo classInfo) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, session.getClassId())
                .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name())
                .orderByAsc(Enrollment::getId));
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                Wrappers.<CheckinRecord>lambdaQuery().eq(CheckinRecord::getSessionId, session.getId()));
        Map<Long, CheckinRecord> recordMap = records.stream()
                .collect(Collectors.toMap(CheckinRecord::getEnrollmentId, Function.identity(), (a, b) -> a));

        Map<Long, SysUser> userMap = enrollments.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(enrollments.stream()
                                .map(Enrollment::getMemberId).distinct().toList())
                        .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        boolean finished = session.getEndTime().isBefore(LocalDateTime.now());
        List<RosterItemVO> items = enrollments.stream().map(enrollment -> {
            CheckinRecord record = recordMap.get(enrollment.getId());
            SysUser user = userMap.get(enrollment.getMemberId());
            String status = record != null ? STATUS_CHECKED
                    : finished ? STATUS_ABSENT : STATUS_NOT_CHECKED;
            return new RosterItemVO(
                    enrollment.getId(),
                    enrollment.getMemberId(),
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getPhone(),
                    status,
                    record == null ? null : record.getCheckinTime(),
                    record == null ? null : record.getSource());
        }).toList();

        return new RosterVO(
                session.getId(),
                session.getClassId(),
                session.getSessionNo(),
                classInfo.getTitle(),
                venueName(session.getVenueId()),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                items.size(),
                items.stream().filter(item -> STATUS_CHECKED.equals(item.getStatus())).count(),
                items);
    }

    private CheckinCodeVO buildCode(ClassSession session, ClassInfo classInfo) {
        long checked = checkinRecordMapper.selectCount(Wrappers.<CheckinRecord>lambdaQuery()
                .eq(CheckinRecord::getSessionId, session.getId()));
        long total = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, session.getClassId())
                .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name()));
        return new CheckinCodeVO(
                session.getId(),
                session.getClassId(),
                session.getSessionNo(),
                classInfo.getTitle(),
                venueName(session.getVenueId()),
                session.getStartTime(),
                session.getEndTime(),
                session.getCheckinCode(),
                session.getCheckinExpireAt(),
                checked,
                total);
    }

    private ClassSession requireSession(Long sessionId) {
        ClassSession session = classSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课次不存在");
        }
        return session;
    }

    private ClassInfo requireCoach(ClassSession session, Long coachUserId) {
        ClassInfo classInfo = classInfoMapper.selectById(session.getClassId());
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        Long ownerId = session.getCoachId() == null ? classInfo.getCoachId() : session.getCoachId();
        if (!Objects.equals(ownerId, coachUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己负责的课次");
        }
        return classInfo;
    }

    private void requireNormal(ClassSession session) {
        if (!SESSION_NORMAL.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课次已取消或已结束");
        }
    }

    private Enrollment findEnrollment(Long classId, Long memberId) {
        return enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, classId)
                .eq(Enrollment::getMemberId, memberId));
    }

    private CheckinRecord findRecord(Long sessionId, Long enrollmentId) {
        return checkinRecordMapper.selectOne(Wrappers.<CheckinRecord>lambdaQuery()
                .eq(CheckinRecord::getSessionId, sessionId)
                .eq(CheckinRecord::getEnrollmentId, enrollmentId));
    }

    private CheckinResultVO duplicate(CheckinRecord record) {
        return new CheckinResultVO(record.getMemberId(), record.getEnrollmentId(),
                true, true, record.getCheckinTime(), "已核销，无需重复提交");
    }

    private String venueName(Long venueId) {
        if (venueId == null) {
            return null;
        }
        Venue venue = venueMapper.selectById(venueId);
        return venue == null ? null : venue.getName();
    }

    private String randomCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(CODE_UPPER_BOUND));
    }
}
