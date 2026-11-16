-- P2 课程与排期：教练等级/专长/荣誉，班级教练课酬，课次代课教练
-- 已在 2026-11-13 执行，换环境重建库时按 01 → 04 顺序执行

USE group_class;

ALTER TABLE coach_profile
    ADD COLUMN level VARCHAR(16) NOT NULL DEFAULT 'JUNIOR' COMMENT '教练等级：JUNIOR/SENIOR/EXPERT' AFTER real_name,
    ADD COLUMN specialty VARCHAR(100) DEFAULT NULL COMMENT '专长，如 羽毛球反手、步法' AFTER level,
    ADD COLUMN achievement VARCHAR(255) DEFAULT NULL COMMENT '资历荣誉' AFTER specialty;

ALTER TABLE class_info
    ADD COLUMN coach_unit_price DECIMAL(10, 2) DEFAULT NULL COMMENT '本班教练每课次课酬' AFTER price;

ALTER TABLE class_session
    ADD COLUMN coach_id BIGINT UNSIGNED DEFAULT NULL COMMENT '代课教练 sys_user.id，空表示用班级默认教练' AFTER venue_id;
