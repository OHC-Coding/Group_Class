-- P3 并发压测账号：member001 ~ member200
-- 密码统一 123456（BCrypt），只用于本地压测，不带上线
-- 可重复执行：INSERT IGNORE 依赖 uk_username / uk_phone 去重

USE group_class;

INSERT IGNORE INTO sys_user (username, password, phone, nickname, role, status)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200
)
SELECT CONCAT('member', LPAD(n, 3, '0')),
       '$2a$10$wtbhTuICSvJrvUPn5.BX3eBmvTy8so73KdBPld8Oijm7ih3kiRs8S',
       CONCAT('139', LPAD(n, 8, '0')),
       CONCAT('压测会员', LPAD(n, 3, '0')),
       'MEMBER',
       1
FROM seq;

-- 验证：应返回 200
-- SELECT COUNT(*) FROM sys_user WHERE username LIKE 'member%';
