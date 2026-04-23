insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 29, 'exam-plan-management:view', '查看考试计划菜单', 'MENU', '/exam-plans'
where not exists (select 1 from admin_permission where id = 29 or permission_code = 'exam-plan-management:view');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 30, 'exam-plan:read', '查询考试计划数据', 'API', '/api/admin/exam-plans'
where not exists (select 1 from admin_permission where id = 30 or permission_code = 'exam-plan:read');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 31, 'exam-plan:create', '新增考试计划', 'API', '/api/admin/exam-plans'
where not exists (select 1 from admin_permission where id = 31 or permission_code = 'exam-plan:create');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 32, 'exam-plan:update', '编辑考试计划', 'API', '/api/admin/exam-plans/{id}'
where not exists (select 1 from admin_permission where id = 32 or permission_code = 'exam-plan:update');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 33, 'exam-plan:range', '维护考试范围', 'API', '/api/admin/exam-plans/{id}/examinees'
where not exists (select 1 from admin_permission where id = 33 or permission_code = 'exam-plan:range');

insert into admin_permission (id, permission_code, permission_name, permission_type, path)
select 34, 'exam-plan:status', '变更考试状态', 'API', '/api/admin/exam-plans/{id}/status'
where not exists (select 1 from admin_permission where id = 34 or permission_code = 'exam-plan:status');

insert into admin_role_permission (id, role_id, permission_id)
select 29, 1, 29
where not exists (select 1 from admin_role_permission where id = 29 or (role_id = 1 and permission_id = 29));

insert into admin_role_permission (id, role_id, permission_id)
select 30, 1, 30
where not exists (select 1 from admin_role_permission where id = 30 or (role_id = 1 and permission_id = 30));

insert into admin_role_permission (id, role_id, permission_id)
select 31, 1, 31
where not exists (select 1 from admin_role_permission where id = 31 or (role_id = 1 and permission_id = 31));

insert into admin_role_permission (id, role_id, permission_id)
select 32, 1, 32
where not exists (select 1 from admin_role_permission where id = 32 or (role_id = 1 and permission_id = 32));

insert into admin_role_permission (id, role_id, permission_id)
select 33, 1, 33
where not exists (select 1 from admin_role_permission where id = 33 or (role_id = 1 and permission_id = 33));

insert into admin_role_permission (id, role_id, permission_id)
select 34, 1, 34
where not exists (select 1 from admin_role_permission where id = 34 or (role_id = 1 and permission_id = 34));
