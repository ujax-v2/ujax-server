CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NULL,
    name VARCHAR(30) NOT NULL,
    profile_image_url VARCHAR(255) NULL,
    provider ENUM('GOOGLE', 'KAKAO', 'LOCAL') NOT NULL,
    provider_id VARCHAR(255) NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    baekjoon_id VARCHAR(255) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE TABLE workspaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200) NULL,
    hook_url VARCHAR(255) NULL,
    image_url VARCHAR(500) NULL,
    CONSTRAINT pk_workspaces PRIMARY KEY (id)
);

CREATE TABLE workspace_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('MANAGER', 'MEMBER', 'OWNER') NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    last_activity_date DATE NULL,
    current_streak_days INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_workspace_members PRIMARY KEY (id),
    CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE TABLE workspace_join_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_workspace_join_requests PRIMARY KEY (id),
    CONSTRAINT uk_wjr_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE TABLE boards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    type ENUM('DATA', 'FREE', 'NOTICE', 'QNA') NOT NULL,
    is_pinned BIT NOT NULL,
    title VARCHAR(50) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    view_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_boards PRIMARY KEY (id)
);

CREATE TABLE board_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_board_comments PRIMARY KEY (id)
);

CREATE TABLE board_likes (
    board_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    is_deleted BIT NOT NULL,
    CONSTRAINT pk_board_likes PRIMARY KEY (board_id, workspace_member_id)
);

CREATE TABLE problem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    problem_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    tier VARCHAR(255) NULL,
    time_limit VARCHAR(255) NULL,
    memory_limit VARCHAR(255) NULL,
    description TEXT NULL,
    input_description TEXT NULL,
    output_description TEXT NULL,
    url VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_problem PRIMARY KEY (id),
    CONSTRAINT uk_problem_problem_number UNIQUE (problem_number)
);

CREATE TABLE algorithm_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_algorithm_tag PRIMARY KEY (id),
    CONSTRAINT uk_algorithm_tag_name UNIQUE (name)
);

CREATE TABLE problem_box (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(30) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_problem_box PRIMARY KEY (id)
);

CREATE TABLE sample (
    id BIGINT NOT NULL AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    sample_index INT NOT NULL,
    input TEXT NULL,
    output TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_sample PRIMARY KEY (id),
    CONSTRAINT uk_sample_problem_sample_index UNIQUE (problem_id, sample_index)
);

CREATE TABLE problem_algorithm (
    problem_id BIGINT NOT NULL,
    algorithm_id BIGINT NOT NULL
);

CREATE TABLE workspace_problem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    problem_box_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    deadline DATETIME(6) NULL,
    scheduled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_workspace_problem PRIMARY KEY (id)
);

CREATE TABLE solution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_problem_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    status ENUM(
        'ACCEPTED',
        'COMPILE_ERROR',
        'MEMORY_LIMIT_EXCEEDED',
        'OTHER',
        'OUTPUT_LIMIT_EXCEEDED',
        'PARTIAL_ACCEPTED',
        'PRESENTATION_ERROR',
        'RUNTIME_ERROR',
        'TIME_LIMIT_EXCEEDED',
        'WRONG_ANSWER'
    ) NOT NULL,
    time VARCHAR(255) NULL,
    memory VARCHAR(255) NULL,
    programming_language ENUM(
        'C',
        'CPP',
        'CSHARP',
        'GO',
        'JAVA',
        'JAVASCRIPT',
        'KOTLIN',
        'OTHER',
        'PYTHON',
        'RUBY',
        'RUST',
        'SWIFT'
    ) NULL,
    code_length VARCHAR(255) NULL,
    code MEDIUMTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_solution PRIMARY KEY (id),
    CONSTRAINT uk_solution_submission_id UNIQUE (submission_id)
);

CREATE TABLE solution_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solution_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_solution_comments PRIMARY KEY (id)
);

CREATE TABLE solution_likes (
    solution_id BIGINT NOT NULL,
    workspace_member_id BIGINT NOT NULL,
    is_deleted BIT NOT NULL,
    CONSTRAINT pk_solution_likes PRIMARY KEY (solution_id, workspace_member_id)
);

CREATE TABLE webhook_alert (
    webhook_alert_id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_problem_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    next_scheduled_at DATETIME(6) NULL,
    status ENUM('PENDING', 'PROCESSING') NOT NULL,
    attempt_no INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_webhook_alert PRIMARY KEY (webhook_alert_id),
    CONSTRAINT uk_webhook_alert_workspace_problem UNIQUE (workspace_problem_id)
);

CREATE TABLE webhook_alert_log (
    webhook_alert_log_id BIGINT NOT NULL AUTO_INCREMENT,
    webhook_alert_id BIGINT NULL,
    workspace_problem_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    event_type ENUM(
        'CANCELLED',
        'CREATED',
        'DEACTIVATED',
        'DEFERRED_SCHEDULED',
        'DELIVERED',
        'FAILED',
        'PROCESSING_STARTED',
        'RECOVERED',
        'RETRY_SCHEDULED',
        'SCHEDULE_UPDATED'
    ) NOT NULL,
    from_status ENUM('PENDING', 'PROCESSING') NULL,
    to_status ENUM('PENDING', 'PROCESSING') NULL,
    scheduled_at DATETIME(6) NULL,
    next_scheduled_at DATETIME(6) NULL,
    attempt_no INT NULL,
    send_at DATETIME(6) NULL,
    last_attempt_at DATETIME(6) NULL,
    err_msg VARCHAR(255) NULL,
    actor_type ENUM('BATCH', 'SYSTEM', 'USER') NOT NULL,
    actor_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_webhook_alert_log PRIMARY KEY (webhook_alert_log_id)
);

CREATE INDEX idx_workspace_members_user_id
    ON workspace_members (user_id);

CREATE INDEX idx_wjr_workspace_created_at
    ON workspace_join_requests (workspace_id, created_at);

CREATE INDEX idx_boards_workspace_created_id
    ON boards (workspace_id, created_at, id);

CREATE INDEX idx_board_comments_board_id
    ON board_comments (board_id);

CREATE INDEX idx_problem_box_workspace_updated_id
    ON problem_box (workspace_id, updated_at, id);

CREATE INDEX idx_workspace_problem_problem_box_id
    ON workspace_problem (problem_box_id);

CREATE INDEX idx_solution_workspace_problem_created_id
    ON solution (workspace_problem_id, created_at, id);

CREATE INDEX idx_solution_comments_solution_created_id
    ON solution_comments (solution_id, created_at, id);

CREATE INDEX idx_webhook_alert_due
    ON webhook_alert (status, scheduled_at);

CREATE INDEX idx_webhook_alert_status_updated_at
    ON webhook_alert (status, updated_at);

CREATE INDEX idx_webhook_alert_log_workspace_problem_created
    ON webhook_alert_log (workspace_problem_id, created_at);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE workspace_members
    ADD CONSTRAINT fk_workspace_members_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

ALTER TABLE workspace_members
    ADD CONSTRAINT fk_workspace_members_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE workspace_join_requests
    ADD CONSTRAINT fk_workspace_join_requests_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

ALTER TABLE workspace_join_requests
    ADD CONSTRAINT fk_workspace_join_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE boards
    ADD CONSTRAINT fk_boards_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

ALTER TABLE boards
    ADD CONSTRAINT fk_boards_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);

ALTER TABLE board_comments
    ADD CONSTRAINT fk_board_comments_board
        FOREIGN KEY (board_id) REFERENCES boards (id);

ALTER TABLE board_comments
    ADD CONSTRAINT fk_board_comments_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);

ALTER TABLE board_likes
    ADD CONSTRAINT fk_board_likes_board
        FOREIGN KEY (board_id) REFERENCES boards (id);

ALTER TABLE board_likes
    ADD CONSTRAINT fk_board_likes_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);

ALTER TABLE problem_box
    ADD CONSTRAINT fk_problem_box_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

ALTER TABLE sample
    ADD CONSTRAINT fk_sample_problem
        FOREIGN KEY (problem_id) REFERENCES problem (id);

ALTER TABLE problem_algorithm
    ADD CONSTRAINT fk_problem_algorithm_problem
        FOREIGN KEY (problem_id) REFERENCES problem (id);

ALTER TABLE problem_algorithm
    ADD CONSTRAINT fk_problem_algorithm_algorithm
        FOREIGN KEY (algorithm_id) REFERENCES algorithm_tag (id);

ALTER TABLE workspace_problem
    ADD CONSTRAINT fk_workspace_problem_problem_box
        FOREIGN KEY (problem_box_id) REFERENCES problem_box (id);

ALTER TABLE workspace_problem
    ADD CONSTRAINT fk_workspace_problem_problem
        FOREIGN KEY (problem_id) REFERENCES problem (id);

ALTER TABLE solution
    ADD CONSTRAINT fk_solution_workspace_problem
        FOREIGN KEY (workspace_problem_id) REFERENCES workspace_problem (id);

ALTER TABLE solution
    ADD CONSTRAINT fk_solution_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);

ALTER TABLE solution_comments
    ADD CONSTRAINT fk_solution_comments_solution
        FOREIGN KEY (solution_id) REFERENCES solution (id);

ALTER TABLE solution_comments
    ADD CONSTRAINT fk_solution_comments_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);

ALTER TABLE solution_likes
    ADD CONSTRAINT fk_solution_likes_solution
        FOREIGN KEY (solution_id) REFERENCES solution (id);

ALTER TABLE solution_likes
    ADD CONSTRAINT fk_solution_likes_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members (id);
