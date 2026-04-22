insert into examinee (id, examinee_no, name, gender, id_card_no, phone, email, status, remark, deleted, created_at, updated_at)
values (1, 'EX2026001', '张三', 'MALE', '110101199001010011', '13800000001', 'zhangsan@example.com', 'ENABLED', '首批考生', 0, now(), now()),
       (2, 'EX2026002', '李四', 'FEMALE', '110101199202020022', '13800000002', 'lisi@example.com', 'DISABLED', '待复核', 0, now(), now()),
       (3, 'EX2026003', '王五', 'MALE', '110101199303030033', '13800000003', 'wangwu@example.com', 'ENABLED', '正式考生', 0, now(), now());
