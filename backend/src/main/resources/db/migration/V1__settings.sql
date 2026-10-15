CREATE TABLE settings (
    id BIGINT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NULL,
    time_zone VARCHAR(64) NOT NULL,
    notifications_enabled BOOLEAN NOT NULL,
    default_start_page VARCHAR(20) NOT NULL,
    configured BOOLEAN NOT NULL
);

INSERT INTO settings (id, display_name, email, time_zone, notifications_enabled, default_start_page, configured)
VALUES (1, 'QuickFlow User', NULL, 'UTC', TRUE, 'DASHBOARD', FALSE);
