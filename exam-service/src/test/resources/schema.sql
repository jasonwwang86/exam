drop table if exists admin_role_permission;
drop table if exists admin_user_role;
drop table if exists admin_session;
drop table if exists exam_plan_examinee;
drop table if exists exam_answer_record;
drop table if exists exam_answer_session;
drop table if exists exam_plan;
drop table if exists paper_question;
drop table if exists paper;
drop table if exists question;
drop table if exists question_type;
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

create table question_type (
    id bigint primary key auto_increment,
    name varchar(64) not null unique,
    answer_mode varchar(32) not null,
    sort_order int not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table question (
    id bigint primary key auto_increment,
    stem varchar(1000) not null,
    question_type_id bigint not null,
    difficulty varchar(16) not null,
    score decimal(6, 2) not null,
    answer_config clob not null,
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table paper (
    id bigint primary key auto_increment,
    name varchar(128) not null unique,
    description varchar(500),
    duration_minutes int not null,
    total_score decimal(8, 2) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table paper_question (
    id bigint primary key auto_increment,
    paper_id bigint not null,
    question_id bigint not null,
    question_stem_snapshot varchar(1000) not null,
    question_type_name_snapshot varchar(64) not null,
    difficulty_snapshot varchar(16) not null,
    answer_config_snapshot clob not null,
    item_score decimal(8, 2) not null,
    display_order int not null,
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table exam_plan (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    paper_id bigint not null,
    start_time timestamp not null,
    end_time timestamp not null,
    status varchar(16) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table exam_plan_examinee (
    id bigint primary key auto_increment,
    exam_plan_id bigint not null,
    examinee_id bigint not null,
    created_at timestamp not null,
    unique (exam_plan_id, examinee_id)
);

create table exam_answer_session (
    id bigint primary key auto_increment,
    exam_plan_id bigint not null,
    examinee_id bigint not null,
    paper_id bigint not null,
    started_at timestamp not null,
    deadline_at timestamp not null,
    status varchar(32) not null,
    last_saved_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique (exam_plan_id, examinee_id)
);

create table exam_answer_record (
    id bigint primary key auto_increment,
    session_id bigint not null,
    paper_question_id bigint not null,
    question_id bigint not null,
    answer_content clob,
    answer_status varchar(32) not null,
    last_saved_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique (session_id, paper_question_id)
);
