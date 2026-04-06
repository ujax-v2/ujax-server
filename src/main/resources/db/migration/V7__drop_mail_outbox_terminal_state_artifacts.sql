DELETE FROM mail_outbox
WHERE status IN ('SENT', 'FAILED');

ALTER TABLE mail_outbox
    DROP COLUMN sent_at;
