freestar
CREATE TABLE IF NOT EXISTS shedlock (
                          name VARCHAR(64),
                          lock_until TIMESTAMP(3) NULL,
                          locked_at TIMESTAMP(3) NULL,
                          locked_by VARCHAR(255),
                          PRIMARY KEY (name)
)
CREATE TABLE IF NOT EXISTS failed_notifications (
                                                    id VARCHAR(36) NOT NULL PRIMARY KEY,
    type VARCHAR(10) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    template_name VARCHAR(100),
    template_data TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_retry TIMESTAMP,
    error_message VARCHAR(500)
    );