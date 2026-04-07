SET @schema_name = DATABASE();

SET @password_hash_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'pending_signups'
      AND column_name = 'password_hash'
);
SET @drop_password_hash_sql = IF(
    @password_hash_exists > 0,
    'ALTER TABLE pending_signups DROP COLUMN password_hash',
    'SELECT 1'
);
PREPARE drop_password_hash_stmt FROM @drop_password_hash_sql;
EXECUTE drop_password_hash_stmt;
DEALLOCATE PREPARE drop_password_hash_stmt;

SET @name_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'pending_signups'
      AND column_name = 'name'
);
SET @drop_name_sql = IF(
    @name_exists > 0,
    'ALTER TABLE pending_signups DROP COLUMN name',
    'SELECT 1'
);
PREPARE drop_name_stmt FROM @drop_name_sql;
EXECUTE drop_name_stmt;
DEALLOCATE PREPARE drop_name_stmt;

SET @expires_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'pending_signups'
      AND index_name = 'idx_pending_signups_expires_at'
);
SET @create_expires_index_sql = IF(
    @expires_index_exists = 0,
    'CREATE INDEX idx_pending_signups_expires_at ON pending_signups (expires_at)',
    'SELECT 1'
);
PREPARE create_expires_index_stmt FROM @create_expires_index_sql;
EXECUTE create_expires_index_stmt;
DEALLOCATE PREPARE create_expires_index_stmt;
