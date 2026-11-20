-- P7 结算与报表：退款单补审批意见
-- 已在 2026-11-20 执行，换环境重建库时按 01 → 04 → 06 → 07 顺序执行

USE group_class;

ALTER TABLE refund_order
    ADD COLUMN audit_remark VARCHAR(200) DEFAULT NULL COMMENT '审批意见' AFTER status;
