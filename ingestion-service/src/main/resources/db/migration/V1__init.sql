-- V1__init.sql
-- Initial schema for Crime Data Ingestion & Analytics

-- USERS
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'ANALYST',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SOURCE SYSTEMS
CREATE TABLE source_systems (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    api_endpoint VARCHAR(200)
);

-- EVENTS
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    source_id INT REFERENCES source_systems(id) ON DELETE SET NULL,
    description TEXT,
    location VARCHAR(255),
--    payload JSONB,
    status VARCHAR(20) DEFAULT 'INGESTED',
    created_at TIMESTAMP DEFAULT NOW()
);

-- EVENT IMAGES
CREATE TABLE event_images (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    url TEXT NOT NULL
);

-- EVENT METADATA
CREATE TABLE event_metadata (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    field_value TEXT
);

-- ANOMALY LABELS
CREATE TABLE anomaly_labels (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- ANOMALIES
CREATE TABLE anomalies (
    id BIGSERIAL PRIMARY KEY,
    label_id INT REFERENCES anomaly_labels(id),
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confidence_score NUMERIC(5,2),
    severity VARCHAR(20),
    description TEXT,
    first_event_id BIGINT REFERENCES events(id),
    last_event_id BIGINT REFERENCES events(id)
);

-- USER-EVENT RELATIONSHIP
CREATE TABLE user_events (
    user_id INT REFERENCES users(id),
    event_id BIGINT REFERENCES events(id),
    operation VARCHAR(50),
    validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, event_id)
);

-- AUDIT LOGS
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    operation VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
