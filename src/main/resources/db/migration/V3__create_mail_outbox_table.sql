CREATE TABLE mail_outbox (
    mail_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
    mail_type ENUM('SIGNUP_VERIFICATION', 'WORKSPACE_INVITE') NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    status ENUM('PENDING', 'PROCESSING') NOT NULL,
    attempt_no INT NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_mail_outbox PRIMARY KEY (mail_outbox_id)
);

CREATE INDEX idx_mail_outbox_due
    ON mail_outbox (status, next_attempt_at);

CREATE INDEX idx_mail_outbox_status_updated_at
    ON mail_outbox (status, updated_at);
