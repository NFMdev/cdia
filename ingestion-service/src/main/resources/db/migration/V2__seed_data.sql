-- V2__seed_data.sql
-- Seed data for CDIA-XCI-project (safe with auto-generated IDs)

-- 1. Users
INSERT INTO users (username, email, password_hash, role)
VALUES
    ('admin', 'admin@example.com', 'TEST1', 'ADMIN'),
    ('analyst1', 'analyst1@example.com', 'TEST2', 'ANALYST');

-- 2. Source Systems
INSERT INTO source_systems (name, description)
VALUES
    ('CCTV-NorthGate', 'Camera at North Gate'),
    ('MobileApp', 'Citizen reporting app'),
    ('IoT-Sensor-001', 'Environmental sensor in District 1'),
    ('SIMULATOR', 'Simulated data source for testing');

-- 3. Anomaly Labels
INSERT INTO anomaly_labels (code, description)
VALUES
    ('UNAUTHORIZED_ACCESS', 'Access attempt without credentials'),
    ('FIRE_DETECTED', 'Possible fire event detected'),
    ('VIOLENCE', 'Physical altercation detected'),
    ('VANDALISM', 'Property damage detected');

-- 4. Events
INSERT INTO events (source_id, description, location)
VALUES
    ((SELECT id FROM source_systems WHERE name = 'CCTV-NorthGate'),
        'Unauthorized access detected', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'CCTV-NorthGate'),
        'Unauthorized access detected', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    ),
    ((SELECT id FROM source_systems WHERE name = 'MobileApp'),
        'Fire reported via mobile app', 'Aalborg'
    );

-- 5. Event Images
INSERT INTO event_images (event_id, url)
VALUES
    (
        1,
        'https://example.com/imgs/event1.jpg'
    ),
    (
        1,
        'https://example.com/imgs/event2.jpg'
    );

-- 6. Audit Logs
INSERT INTO audit_logs (user_id, operation, entity_type, entity_id, performed_at)
VALUES
    (
        (SELECT id FROM users WHERE username = 'admin'),
        'LOGIN',
        'USERS',
        1,
        NOW()
    ),
    (
        (SELECT id FROM users WHERE username = 'admin'),
        'CREATE_EVENT',
        'EVENTS',
        1,
        NOW()
    );