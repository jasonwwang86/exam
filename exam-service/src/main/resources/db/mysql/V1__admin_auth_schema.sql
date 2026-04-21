create table if not exists admin_user (
    id bigint primary key auto_increment,
    username varchar(64) not null unique,
    password_hash varchar(128) not null,
    display_name varchar(128) not null,
    enabled tinyint not null default 1
);

create table if not exists admin_role (
    id bigint primary key auto_increment,
    role_code varchar(64) not null unique,
    role_name varchar(128) not null
);

create table if not exists admin_permission (
    id bigint primary key auto_increment,
    permission_code varchar(128) not null unique,
    permission_name varchar(128) not null,
    permission_type varchar(32) not null,
    path varchar(255)
);

create table if not exists admin_user_role (
    id bigint primary key auto_increment,
    user_id bigint not null,
    role_id bigint not null,
    unique key uk_admin_user_role (user_id, role_id)
);

create table if not exists admin_role_permission (
    id bigint primary key auto_increment,
    role_id bigint not null,
    permission_id bigint not null,
    unique key uk_admin_role_permission (role_id, permission_id)
);

create table if not exists admin_session (
    id bigint primary key auto_increment,
    user_id bigint not null,
    token varchar(128) not null unique,
    expires_at datetime not null,
    last_active_at datetime not null,
    revoked tinyint not null default 0
);

create table if not exists examinee (
    id bigint primary key auto_increment,
    examinee_no varchar(64) not null,
    name varchar(64) not null,
    gender varchar(16) not null,
    id_card_no varchar(32) not null,
    phone varchar(32) not null,
    email varchar(128),
    status varchar(16) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_examinee_no (examinee_no),
    unique key uk_examinee_id_card_no (id_card_no)
);
