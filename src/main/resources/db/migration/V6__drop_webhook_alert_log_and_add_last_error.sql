ALTER TABLE webhook_alert
    ADD COLUMN last_error VARCHAR(500) NULL;

DROP TABLE webhook_alert_log;
