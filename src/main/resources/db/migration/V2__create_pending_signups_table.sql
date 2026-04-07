CREATE TABLE pending_signups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    request_token VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(30) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_pending_signups PRIMARY KEY (id),
    CONSTRAINT uk_pending_signups_request_token UNIQUE (request_token),
    CONSTRAINT uk_pending_signups_email UNIQUE (email)
);
