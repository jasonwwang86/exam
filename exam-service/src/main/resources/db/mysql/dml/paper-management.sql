insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 20, 'paper-management:view', '查看试卷管理菜单', 'MENU', '/papers'
where not exists (select 1 from admin_permission where id = 20 or permission_code = 'paper-management:view');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 21, 'paper:read', '查询试卷数据', 'API', '/api/admin/papers'
where not exists (select 1 from admin_permission where id = 21 or permission_code = 'paper:read');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 22, 'paper:create', '新增试卷', 'API', '/api/admin/papers'
where not exists (select 1 from admin_permission where id = 22 or permission_code = 'paper:create');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 23, 'paper:update', '编辑试卷', 'API', '/api/admin/papers/{id}'
where not exists (select 1 from admin_permission where id = 23 or permission_code = 'paper:update');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 24, 'paper:delete', '删除试卷', 'API', '/api/admin/papers/{id}'
where not exists (select 1 from admin_permission where id = 24 or permission_code = 'paper:delete');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 25, 'paper-question:read', '查询试卷题目明细', 'API', '/api/admin/papers/{id}/questions'
where not exists (select 1 from admin_permission where id = 25 or permission_code = 'paper-question:read');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 26, 'paper-question:create', '新增试卷题目明细', 'API', '/api/admin/papers/{id}/questions'
where not exists (select 1 from admin_permission where id = 26 or permission_code = 'paper-question:create');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 27, 'paper-question:update', '编辑试卷题目明细', 'API', '/api/admin/papers/{id}/questions/{paperQuestionId}'
where not exists (select 1 from admin_permission where id = 27 or permission_code = 'paper-question:update');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 28, 'paper-question:delete', '删除试卷题目明细', 'API', '/api/admin/papers/{id}/questions/{paperQuestionId}'
where not exists (select 1 from admin_permission where id = 28 or permission_code = 'paper-question:delete');

insert into admin_role_permission (id, role_id, permission_id)
select 20, 1, 20
where not exists (select 1 from admin_role_permission where id = 20 or (role_id = 1 and permission_id = 20));

insert into admin_role_permission (id, role_id, permission_id)
select 21, 1, 21
where not exists (select 1 from admin_role_permission where id = 21 or (role_id = 1 and permission_id = 21));

insert into admin_role_permission (id, role_id, permission_id)
select 22, 1, 22
where not exists (select 1 from admin_role_permission where id = 22 or (role_id = 1 and permission_id = 22));

insert into admin_role_permission (id, role_id, permission_id)
select 23, 1, 23
where not exists (select 1 from admin_role_permission where id = 23 or (role_id = 1 and permission_id = 23));

insert into admin_role_permission (id, role_id, permission_id)
select 24, 1, 24
where not exists (select 1 from admin_role_permission where id = 24 or (role_id = 1 and permission_id = 24));

insert into admin_role_permission (id, role_id, permission_id)
select 25, 1, 25
where not exists (select 1 from admin_role_permission where id = 25 or (role_id = 1 and permission_id = 25));

insert into admin_role_permission (id, role_id, permission_id)
select 26, 1, 26
where not exists (select 1 from admin_role_permission where id = 26 or (role_id = 1 and permission_id = 26));

insert into admin_role_permission (id, role_id, permission_id)
select 27, 1, 27
where not exists (select 1 from admin_role_permission where id = 27 or (role_id = 1 and permission_id = 27));

insert into admin_role_permission (id, role_id, permission_id)
select 28, 1, 28
where not exists (select 1 from admin_role_permission where id = 28 or (role_id = 1 and permission_id = 28));
