CREATE TABLE pending_signups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    request_token VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_pending_signups PRIMARY KEY (id),
    CONSTRAINT uk_pending_signups_request_token UNIQUE (request_token),
    CONSTRAINT uk_pending_signups_email UNIQUE (email)
);

CREATE INDEX idx_pending_signups_expires_at
    ON pending_signups (expires_at);
