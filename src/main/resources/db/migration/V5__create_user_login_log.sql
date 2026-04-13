CREATE TABLE user_login_log (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    logged_in_at DATETIME NOT NULL,
    CONSTRAINT pk_user_login_log PRIMARY KEY (id),
    INDEX idx_user_login_log_logged_in_at (logged_in_at),
    INDEX idx_user_login_log_user_logged_in (user_id, logged_in_at)
);
