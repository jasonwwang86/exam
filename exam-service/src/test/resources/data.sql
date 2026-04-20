insert into admin_role (id, role_code, role_name)
values (1, 'SUPER_ADMIN', '超级管理员');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
values (1, 'dashboard:view', '查看管理端首页', 'MENU', '/dashboard'),
       (2, 'dashboard:read', '读取管理端首页数据', 'API', '/api/admin/dashboard/summary');

insert into admin_user (id, username, password_hash, display_name, enabled)
values (1, 'admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '系统管理员', 1),
       (2, 'disabled-admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '停用管理员', 0),
       (3, 'limited-admin', '$2a$10$PZc9xahNxphTLeeyo6Ezv.trTKZERaKBHGZGzR/EF73f1fTZxB032', '受限管理员', 1);

insert into admin_user_role (id, user_id, role_id)
values (1, 1, 1);

insert into admin_role_permission (id, role_id, permission_id)
values (1, 1, 1),
       (2, 1, 2);
