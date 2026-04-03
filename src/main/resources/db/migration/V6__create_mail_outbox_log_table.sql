CREATE TABLE mail_outbox_log (
    mail_outbox_log_id BIGINT NOT NULL AUTO_INCREMENT,
    mail_outbox_id BIGINT NOT NULL,
    mail_type ENUM('SIGNUP_VERIFICATION', 'WORKSPACE_INVITE') NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    event_type ENUM('ENQUEUED', 'PROCESSING_STARTED', 'RECOVERED', 'SENT', 'RETRY_SCHEDULED', 'FAILED') NOT NULL,
    from_status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED') NULL,
    to_status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED') NULL,
    attempt_no INT NULL,
    next_attempt_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_mail_outbox_log PRIMARY KEY (mail_outbox_log_id)
);

CREATE INDEX idx_mail_outbox_log_outbox_created
    ON mail_outbox_log (mail_outbox_id, created_at);
