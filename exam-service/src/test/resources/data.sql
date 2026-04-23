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
       (19, 'question-type:delete', '删除题型', 'API', '/api/admin/question-types/{id}'),
       (20, 'paper-management:view', '查看试卷管理菜单', 'MENU', '/papers'),
       (21, 'paper:read', '查询试卷数据', 'API', '/api/admin/papers'),
       (22, 'paper:create', '新增试卷', 'API', '/api/admin/papers'),
       (23, 'paper:update', '编辑试卷', 'API', '/api/admin/papers/{id}'),
       (24, 'paper:delete', '删除试卷', 'API', '/api/admin/papers/{id}'),
       (25, 'paper-question:read', '查询试卷题目明细', 'API', '/api/admin/papers/{id}/questions'),
       (26, 'paper-question:create', '新增试卷题目明细', 'API', '/api/admin/papers/{id}/questions'),
       (27, 'paper-question:update', '编辑试卷题目明细', 'API', '/api/admin/papers/{id}/questions/{paperQuestionId}'),
       (28, 'paper-question:delete', '删除试卷题目明细', 'API', '/api/admin/papers/{id}/questions/{paperQuestionId}'),
       (29, 'exam-plan-management:view', '查看考试计划菜单', 'MENU', '/exam-plans'),
       (30, 'exam-plan:read', '查询考试计划数据', 'API', '/api/admin/exam-plans'),
       (31, 'exam-plan:create', '新增考试计划', 'API', '/api/admin/exam-plans'),
       (32, 'exam-plan:update', '编辑考试计划', 'API', '/api/admin/exam-plans/{id}'),
       (33, 'exam-plan:range', '维护考试范围', 'API', '/api/admin/exam-plans/{id}/examinees'),
       (34, 'exam-plan:status', '变更考试状态', 'API', '/api/admin/exam-plans/{id}/status');

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
       (19, 1, 19),
       (20, 1, 20),
       (21, 1, 21),
       (22, 1, 22),
       (23, 1, 23),
       (24, 1, 24),
       (25, 1, 25),
       (26, 1, 26),
       (27, 1, 27),
       (28, 1, 28),
       (29, 1, 29),
       (30, 1, 30),
       (31, 1, 31),
       (32, 1, 32),
       (33, 1, 33),
       (34, 1, 34);

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

insert into paper (id, name, description, duration_minutes, total_score, remark, deleted, created_at, updated_at)
values (1, 'Java 基础试卷', '覆盖 Java 基础知识', 120, 11.00, '首套试卷', 0, current_timestamp, current_timestamp),
       (2, '空白练习卷', '待手工组卷', 90, 0.00, '草拟中', 0, current_timestamp, current_timestamp);

insert into paper_question (id, paper_id, question_id, question_stem_snapshot, question_type_name_snapshot, difficulty_snapshot, item_score, display_order, deleted, created_at, updated_at)
values (1, 1, 1, 'Java 的入口方法是什么？', '单选题', 'EASY', 5.00, 1, 0, current_timestamp, current_timestamp),
       (2, 1, 2, '请写出 JVM 的英文全称。', '简答题', 'MEDIUM', 6.00, 2, 0, current_timestamp, current_timestamp);

insert into exam_plan (id, name, paper_id, start_time, end_time, status, remark, deleted, created_at, updated_at)
values (1, 'Java 基础考试-上午场', 1, timestamp '2026-05-01 09:00:00', timestamp '2026-05-01 12:00:00', 'PUBLISHED', '首场安排', 0, current_timestamp, current_timestamp),
       (2, '待安排考试', 2, timestamp '2026-05-02 09:00:00', timestamp '2026-05-02 11:00:00', 'DRAFT', '待设置范围', 0, current_timestamp, current_timestamp);

insert into exam_plan_examinee (id, exam_plan_id, examinee_id, created_at)
values (1, 1, 1, current_timestamp),
       (2, 1, 3, current_timestamp);
