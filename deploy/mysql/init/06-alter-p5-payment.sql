-- P5 支付：回调编号作为幂等键，唯一索引兜底
-- 已在 2026-11-20 执行，换环境重建库时按 01 → 04 → 06 顺序执行

USE group_class;

ALTER TABLE payment_order
    ADD UNIQUE KEY uk_notify_no (notify_no);
