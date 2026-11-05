USE group_class;

-- 可重复执行：已存在则跳过

INSERT INTO venue (name, address, capacity)
SELECT '一号场', 'A 馆 1 楼', 12 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM venue WHERE name = '一号场');

INSERT INTO venue (name, address, capacity)
SELECT '二号场', 'A 馆 2 楼', 10 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM venue WHERE name = '二号场');

INSERT INTO venue (name, address, capacity)
SELECT '三号场', 'B 馆 1 楼', 16 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM venue WHERE name = '三号场');

INSERT INTO course_template (name, sport_type, default_sessions, default_price, description)
SELECT '高水平提升课', '羽毛球', 5, 800.00, '周末连上五天，进阶对抗与体能' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM course_template WHERE name = '高水平提升课');

INSERT INTO course_template (name, sport_type, default_sessions, default_price, description)
SELECT '零基础入门课', '羽毛球', 6, 600.00, '握拍、步法与高远球' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM course_template WHERE name = '零基础入门课');

INSERT INTO course_template (name, sport_type, default_sessions, default_price, description)
SELECT '少儿兴趣班', '羽毛球', 8, 720.00, '面向 8-12 岁学员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM course_template WHERE name = '少儿兴趣班');
