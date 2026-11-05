USE group_class;

-- 开发环境测试账号，密码统一 123456，上线前删掉

INSERT INTO sys_user (username, password, phone, nickname, role, status)
SELECT 'admin', '$2a$10$wtbhTuICSvJrvUPn5.BX3eBmvTy8so73KdBPld8Oijm7ih3kiRs8S', '13800000001', '系统管理员', 'ADMIN', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, password, phone, nickname, role, status)
SELECT 'coach1', '$2a$10$wtbhTuICSvJrvUPn5.BX3eBmvTy8so73KdBPld8Oijm7ih3kiRs8S', '13800000002', '张教练', 'COACH', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'coach1');

INSERT INTO sys_user (username, password, phone, nickname, role, status)
SELECT 'member1', '$2a$10$wtbhTuICSvJrvUPn5.BX3eBmvTy8so73KdBPld8Oijm7ih3kiRs8S', '13800000003', '测试会员', 'MEMBER', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'member1');

-- 给 coach1 配一份教练档案，P2 建班时直接可选用
INSERT INTO coach_profile (user_id, real_name, unit_price, intro, status)
SELECT u.id, '张伟', 120.00, '羽毛球基础与提升课，校队退役，5 年青少年教学经验', 1
FROM sys_user u
WHERE u.username = 'coach1'
  AND NOT EXISTS (SELECT 1 FROM coach_profile p WHERE p.user_id = u.id);