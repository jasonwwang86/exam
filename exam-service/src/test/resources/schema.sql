drop table if exists admin_role_permission;
drop table if exists admin_user_role;
drop table if exists admin_session;
drop table if exists examinee;
drop table if exists admin_permission;
drop table if exists admin_role;
drop table if exists admin_user;

create table admin_user (
    id bigint primary key auto_increment,
    username varchar(64) not null unique,
    password_hash varchar(128) not null,
    display_name varchar(128) not null,
    enabled tinyint not null default 1
);

create table admin_role (
    id bigint primary key auto_increment,
    role_code varchar(64) not null unique,
    role_name varchar(128) not null
);

create table admin_permission (
    id bigint primary key auto_increment,
    permission_code varchar(128) not null unique,
    permission_name varchar(128) not null,
    permission_type varchar(32) not null,
    path varchar(255)
);

create table admin_user_role (
    id bigint primary key auto_increment,
    user_id bigint not null,
    role_id bigint not null
);

create table admin_role_permission (
    id bigint primary key auto_increment,
    role_id bigint not null,
    permission_id bigint not null
);

create table admin_session (
    id bigint primary key auto_increment,
    user_id bigint not null,
    token varchar(128) not null unique,
    expires_at timestamp not null,
    last_active_at timestamp not null,
    revoked tinyint not null default 0
);

create table examinee (
    id bigint primary key auto_increment,
    examinee_no varchar(64) not null unique,
    name varchar(64) not null,
    gender varchar(16) not null,
    id_card_no varchar(32) not null unique,
    phone varchar(32) not null,
    email varchar(128),
    status varchar(16) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);
