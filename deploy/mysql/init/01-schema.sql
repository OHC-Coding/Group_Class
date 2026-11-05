-- 团课预约与核销系统 建表脚本
-- MySQL 8.0+ / 9.x
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < 01-schema.sql

CREATE DATABASE IF NOT EXISTS group_class
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE group_class;

-- ---------- 账号 ----------

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL COMMENT '登录账号',
    password    VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密文',
    phone       VARCHAR(20)     NOT NULL COMMENT '手机号',
    nickname    VARCHAR(50)              DEFAULT NULL,
    role        VARCHAR(20)     NOT NULL COMMENT 'MEMBER / COACH / ADMIN',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账号，三类角色共表';

CREATE TABLE IF NOT EXISTS coach_profile (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT 'sys_user.id',
    real_name   VARCHAR(50)     NOT NULL,
    unit_price  DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '每课次结算单价',
    intro       VARCHAR(500)             DEFAULT NULL,
    status      TINYINT         NOT NULL DEFAULT 1,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教练档案';

-- ---------- 课程 ----------

CREATE TABLE IF NOT EXISTS course_template (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100)    NOT NULL COMMENT '课程名称',
    sport_type       VARCHAR(30)              DEFAULT NULL COMMENT '体育/美术/舞蹈等',
    default_sessions INT             NOT NULL DEFAULT 1 COMMENT '默认总课次',
    default_price    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    description      VARCHAR(500)             DEFAULT NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程模板';

CREATE TABLE IF NOT EXISTS venue (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)     NOT NULL,
    address    VARCHAR(200)             DEFAULT NULL,
    capacity   INT             NOT NULL DEFAULT 0,
    status     TINYINT         NOT NULL DEFAULT 1,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_venue_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场地';

CREATE TABLE IF NOT EXISTS class_info (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_no          VARCHAR(32)     NOT NULL COMMENT '班级编号',
    template_id       BIGINT UNSIGNED NOT NULL,
    title             VARCHAR(100)    NOT NULL COMMENT '班级名称，如 羽毛球提升课·张教练班',
    coach_id          BIGINT UNSIGNED NOT NULL COMMENT 'sys_user.id，角色 COACH',
    venue_id          BIGINT UNSIGNED NOT NULL,
    start_date        DATE            NOT NULL,
    end_date          DATE            NOT NULL,
    start_time        TIME            NOT NULL COMMENT '每次上课开始时间',
    end_time          TIME            NOT NULL,
    total_sessions    INT             NOT NULL COMMENT '总课次',
    capacity          INT             NOT NULL COMMENT '名额',
    price             DECIMAL(10, 2)  NOT NULL,
    enroll_open_time  DATETIME        NOT NULL COMMENT '开放报名时间',
    enroll_close_time DATETIME        NOT NULL COMMENT '截止报名时间',
    status            VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/OPEN/CLOSED/FINISHED/CANCELLED',
    remark            VARCHAR(500)             DEFAULT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_no (class_no),
    KEY idx_coach (coach_id),
    KEY idx_status_open (status, enroll_open_time),
    KEY idx_template (template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级，交易单位';

CREATE TABLE IF NOT EXISTS class_session (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id          BIGINT UNSIGNED NOT NULL,
    session_no        INT             NOT NULL COMMENT '第几次课',
    start_time        DATETIME        NOT NULL,
    end_time          DATETIME        NOT NULL,
    venue_id          BIGINT UNSIGNED NOT NULL,
    checkin_code      VARCHAR(16)              DEFAULT NULL COMMENT '课次签到码',
    checkin_expire_at DATETIME                 DEFAULT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/CANCELLED/FINISHED',
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_session_no (class_id, session_no),
    KEY idx_checkin_code (checkin_code),
    KEY idx_start_time (start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课次，履约单位';

-- ---------- 报名与交易 ----------

CREATE TABLE IF NOT EXISTS enrollment (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    class_id    BIGINT UNSIGNED NOT NULL,
    member_id   BIGINT UNSIGNED NOT NULL COMMENT 'sys_user.id，角色 MEMBER',
    status      VARCHAR(24)     NOT NULL DEFAULT 'PENDING_PAYMENT'
                COMMENT 'PENDING_PAYMENT/CONFIRMED/CANCELLED/REFUNDED',
    enroll_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancel_time DATETIME                 DEFAULT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_member (class_id, member_id),
    KEY idx_member (member_id),
    KEY idx_class_status (class_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报名；取消后再次报名复用同一行';

CREATE TABLE IF NOT EXISTS payment_order (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(32)     NOT NULL COMMENT '支付单号',
    enrollment_id BIGINT UNSIGNED NOT NULL,
    member_id     BIGINT UNSIGNED NOT NULL,
    amount        DECIMAL(10, 2)  NOT NULL,
    channel       VARCHAR(20)     NOT NULL DEFAULT 'MOCK',
    status        VARCHAR(20)     NOT NULL DEFAULT 'PAYING' COMMENT 'PAYING/PAID/CLOSED/REFUNDED',
    expire_time   DATETIME        NOT NULL COMMENT '支付截止时间',
    pay_time      DATETIME                 DEFAULT NULL,
    notify_no     VARCHAR(64)              DEFAULT NULL COMMENT '渠道回调编号，幂等键',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_enrollment (enrollment_id),
    KEY idx_status_expire (status, expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付单';

CREATE TABLE IF NOT EXISTS refund_order (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_no       VARCHAR(32)     NOT NULL,
    enrollment_id   BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    amount          DECIMAL(10, 2)  NOT NULL COMMENT '退款金额',
    total_sessions  INT             NOT NULL COMMENT '班级总课次',
    checked_sessions INT            NOT NULL COMMENT '已核销课次',
    reason          VARCHAR(200)             DEFAULT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'APPLYING' COMMENT 'APPLYING/APPROVED/REJECTED/REFUNDED',
    refund_time     DATETIME                 DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_enrollment (enrollment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退款单';

-- ---------- 履约与结算 ----------

CREATE TABLE IF NOT EXISTS checkin_record (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id    BIGINT UNSIGNED NOT NULL,
    class_id      BIGINT UNSIGNED NOT NULL,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    member_id     BIGINT UNSIGNED NOT NULL,
    coach_id      BIGINT UNSIGNED NOT NULL,
    checkin_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source        VARCHAR(16)     NOT NULL DEFAULT 'CODE' COMMENT 'CODE/MANUAL',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_enrollment (session_id, enrollment_id),
    KEY idx_enrollment (enrollment_id),
    KEY idx_class_member (class_id, member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '核销记录，同课次同报名唯一';

CREATE TABLE IF NOT EXISTS settlement_bill (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    bill_no        VARCHAR(32)     NOT NULL,
    coach_id       BIGINT UNSIGNED NOT NULL,
    period_start   DATE            NOT NULL,
    period_end     DATE            NOT NULL,
    session_count  INT             NOT NULL COMMENT '已核销课次',
    unit_price     DECIMAL(10, 2)  NOT NULL,
    amount         DECIMAL(10, 2)  NOT NULL,
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no),
    UNIQUE KEY uk_coach_period (coach_id, period_start, period_end)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教练课时结算单';

CREATE TABLE IF NOT EXISTS reconciliation_record (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_date       DATE            NOT NULL COMMENT '业务日期',
    enroll_count   INT             NOT NULL DEFAULT 0,
    paid_count     INT             NOT NULL DEFAULT 0,
    occupied_count INT             NOT NULL DEFAULT 0,
    redis_remain   INT             NOT NULL DEFAULT 0,
    diff_count     INT             NOT NULL DEFAULT 0,
    result         VARCHAR(16)     NOT NULL DEFAULT 'PASS' COMMENT 'PASS/FAIL/REPAIRED',
    detail         VARCHAR(1000)            DEFAULT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_date (biz_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日对账';

CREATE TABLE IF NOT EXISTS idempotency_record (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_key    VARCHAR(64)     NOT NULL COMMENT '业务唯一键，如回调编号 / 签到码+报名',
    biz_type   VARCHAR(32)     NOT NULL,
    result     VARCHAR(500)             DEFAULT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_key (biz_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通用幂等记录';
