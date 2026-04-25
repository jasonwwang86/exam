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

create table if not exists question_type (
    id bigint primary key auto_increment,
    name varchar(64) not null,
    answer_mode varchar(32) not null,
    sort_order int not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_question_type_name (name)
);

create table if not exists question (
    id bigint primary key auto_increment,
    stem varchar(1000) not null,
    question_type_id bigint not null,
    difficulty varchar(16) not null,
    score decimal(6, 2) not null,
    answer_config json not null,
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    key idx_question_question_type_id (question_type_id),
    key idx_question_difficulty (difficulty)
);

create table if not exists paper (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    description varchar(500),
    duration_minutes int not null,
    total_score decimal(8, 2) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_paper_name (name)
);

create table if not exists paper_question (
    id bigint primary key auto_increment,
    paper_id bigint not null,
    question_id bigint not null,
    question_stem_snapshot varchar(1000) not null,
    question_type_name_snapshot varchar(64) not null,
    difficulty_snapshot varchar(16) not null,
    answer_config_snapshot json not null,
    item_score decimal(8, 2) not null,
    display_order int not null,
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    key idx_paper_question_paper_id (paper_id),
    key idx_paper_question_question_id (question_id),
    key idx_paper_question_display_order (paper_id, display_order)
);

create table if not exists exam_plan (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    paper_id bigint not null,
    start_time datetime not null,
    end_time datetime not null,
    status varchar(16) not null,
    remark varchar(255),
    deleted tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    key idx_exam_plan_paper_id (paper_id),
    key idx_exam_plan_status (status),
    key idx_exam_plan_start_time (start_time)
);

create table if not exists exam_plan_examinee (
    id bigint primary key auto_increment,
    exam_plan_id bigint not null,
    examinee_id bigint not null,
    created_at datetime not null,
    unique key uk_exam_plan_examinee (exam_plan_id, examinee_id),
    key idx_exam_plan_examinee_examinee_id (examinee_id)
);

create table if not exists exam_result (
    id bigint primary key auto_increment,
    exam_plan_id bigint not null,
    examinee_id bigint not null,
    session_id bigint not null,
    paper_id bigint not null,
    score_status varchar(32) not null,
    total_score decimal(8, 2) not null,
    objective_score decimal(8, 2) null,
    subjective_score decimal(8, 2) null,
    answered_count int not null,
    unanswered_count int not null,
    submitted_at datetime not null,
    generated_at datetime not null,
    published_at datetime null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_exam_result_plan_examinee (exam_plan_id, examinee_id),
    key idx_exam_result_session_id (session_id),
    key idx_exam_result_score_status (score_status)
);

create table if not exists exam_result_item (
    id bigint primary key auto_increment,
    result_id bigint not null,
    paper_question_id bigint not null,
    question_id bigint not null,
    question_no int not null,
    question_stem_snapshot varchar(1000) not null,
    question_type_name_snapshot varchar(64) not null,
    item_score decimal(8, 2) not null,
    awarded_score decimal(8, 2) not null,
    answer_status varchar(32) not null,
    answer_summary text null,
    judge_status varchar(32) not null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_exam_result_item_result_question (result_id, paper_question_id),
    key idx_exam_result_item_result_id_question_no (result_id, question_no)
);

create table if not exists exam_answer_session (
    id bigint primary key auto_increment,
    exam_plan_id bigint not null,
    examinee_id bigint not null,
    paper_id bigint not null,
    started_at datetime not null,
    deadline_at datetime not null,
    status varchar(32) not null,
    last_saved_at datetime null,
    submitted_at datetime null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_exam_answer_session (exam_plan_id, examinee_id),
    key idx_exam_answer_session_examinee_id (examinee_id),
    key idx_exam_answer_session_status (status),
    key idx_exam_answer_session_status_deadline (status, deadline_at)
);

create table if not exists exam_answer_record (
    id bigint primary key auto_increment,
    session_id bigint not null,
    paper_question_id bigint not null,
    question_id bigint not null,
    answer_content json null,
    answer_status varchar(32) not null,
    last_saved_at datetime not null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_exam_answer_record (session_id, paper_question_id),
    key idx_exam_answer_record_question_id (question_id),
    key idx_exam_answer_record_status (answer_status)
);
