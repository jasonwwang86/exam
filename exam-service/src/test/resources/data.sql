insert into admin_role (id, role_code, role_name)
values (1, 'SUPER_ADMIN', '超级管理员');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
values (1, 'dashboard:view', '查看管理端首页', 'MENU', '/dashboard'),
       (2, 'dashboard:read', '读取管理端首页数据', 'API', '/api/admin/dashboard/summary'),
       (3, 'examinee:view', '查看考生管理菜单', 'MENU', '/examinees'),
       (4, 'examinee:read', '查询考生数据', 'API', '/api/admin/examinees'),
       (5, 'examinee:create', '新增考生', 'API', '/api/admin/examinees'),
       (6, 'examinee:update', '编辑考生', 'API', '/api/admin/examinees/{id}'),
       (7, 'examinee:delete', '删除考生', 'API', '/api/admin/examinees/{id}'),
       (8, 'examinee:status', '变更考生状态', 'API', '/api/admin/examinees/{id}/status'),
       (9, 'examinee:import', '批量导入考生', 'API', '/api/admin/examinees/import'),
       (10, 'examinee:export', '导出考生数据', 'API', '/api/admin/examinees/export'),
       (11, 'question-bank:view', '查看题库管理菜单', 'MENU', '/question-bank'),
       (12, 'question:read', '查询题目数据', 'API', '/api/admin/questions'),
       (13, 'question:create', '新增题目', 'API', '/api/admin/questions'),
       (14, 'question:update', '编辑题目', 'API', '/api/admin/questions/{id}'),
       (15, 'question:delete', '删除题目', 'API', '/api/admin/questions/{id}'),
       (16, 'question-type:read', '查询题型数据', 'API', '/api/admin/question-types'),
       (17, 'question-type:create', '新增题型', 'API', '/api/admin/question-types'),
       (18, 'question-type:update', '编辑题型', 'API', '/api/admin/question-types/{id}'),
       (19, 'question-type:delete', '删除题型', 'API', '/api/admin/question-types/{id}');

insert into admin_user (id, username, password_hash, display_name, enabled)
values (1, 'admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '系统管理员', 1),
       (2, 'disabled-admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '停用管理员', 0),
       (3, 'limited-admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '受限管理员', 1);

insert into admin_user_role (id, user_id, role_id)
values (1, 1, 1);

insert into admin_role_permission (id, role_id, permission_id)
values (1, 1, 1),
       (2, 1, 2),
       (3, 1, 3),
       (4, 1, 4),
       (5, 1, 5),
       (6, 1, 6),
       (7, 1, 7),
       (8, 1, 8),
       (9, 1, 9),
       (10, 1, 10),
       (11, 1, 11),
       (12, 1, 12),
       (13, 1, 13),
       (14, 1, 14),
       (15, 1, 15),
       (16, 1, 16),
       (17, 1, 17),
       (18, 1, 18),
       (19, 1, 19);

insert into examinee (id, examinee_no, name, gender, id_card_no, phone, email, status, remark, deleted, created_at, updated_at)
values (1, 'EX2026001', '张三', 'MALE', '110101199001010011', '13800000001', 'zhangsan@example.com', 'ENABLED', '首批考生', 0, current_timestamp, current_timestamp),
       (2, 'EX2026002', '李四', 'FEMALE', '110101199202020022', '13800000002', 'lisi@example.com', 'DISABLED', '待复核', 0, current_timestamp, current_timestamp),
       (3, 'EX2026003', '王五', 'MALE', '110101199303030033', '13800000003', 'wangwu@example.com', 'ENABLED', '正式考生', 0, current_timestamp, current_timestamp);

insert into question_type (id, name, answer_mode, sort_order, remark, deleted, created_at, updated_at)
values (1, '单选题', 'SINGLE_CHOICE', 10, '唯一正确答案', 0, current_timestamp, current_timestamp),
       (2, '多选题', 'MULTIPLE_CHOICE', 20, '多个正确答案', 0, current_timestamp, current_timestamp),
       (3, '判断题', 'TRUE_FALSE', 30, '布尔型答案', 0, current_timestamp, current_timestamp),
       (4, '简答题', 'TEXT', 40, '文本参考答案', 0, current_timestamp, current_timestamp);

insert into question (id, stem, question_type_id, difficulty, score, answer_config, deleted, created_at, updated_at)
values (1, 'Java 的入口方法是什么？', 1, 'EASY', 5.00, '{"options":[{"key":"A","content":"main"},{"key":"B","content":"run"},{"key":"C","content":"start"},{"key":"D","content":"boot"}],"correctOption":"A"}', 0, current_timestamp, current_timestamp),
       (2, '请写出 JVM 的英文全称。', 4, 'MEDIUM', 6.00, '{"acceptedAnswers":["Java Virtual Machine","Java虚拟机"]}', 0, current_timestamp, current_timestamp),
       (3, 'Java 是解释型语言。', 3, 'EASY', 2.00, '{"correctAnswer":false}', 0, current_timestamp, current_timestamp);
