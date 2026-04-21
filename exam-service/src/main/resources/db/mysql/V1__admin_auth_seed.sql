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
       (10, 'examinee:export', '导出考生数据', 'API', '/api/admin/examinees/export');

insert into admin_user (id, username, password_hash, display_name, enabled)
values (1, 'admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '系统管理员', 1);

insert into admin_user (id, username, password_hash, display_name, enabled)
values (2, 'limited-admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '受限管理员', 1);

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
       (10, 1, 10);

insert into examinee (id, examinee_no, name, gender, id_card_no, phone, email, status, remark, deleted, created_at, updated_at)
values (1, 'EX2026001', '张三', 'MALE', '110101199001010011', '13800000001', 'zhangsan@example.com', 'ENABLED', '首批考生', 0, now(), now()),
       (2, 'EX2026002', '李四', 'FEMALE', '110101199202020022', '13800000002', 'lisi@example.com', 'DISABLED', '待复核', 0, now(), now()),
       (3, 'EX2026003', '王五', 'MALE', '110101199303030033', '13800000003', 'wangwu@example.com', 'ENABLED', '正式考生', 0, now(), now());
