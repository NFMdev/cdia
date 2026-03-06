-- Ensure event-time and key fields required by Flink anomaly detection are always present.

UPDATE events
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE events
SET location = 'UNKNOWN'
WHERE location IS NULL OR BTRIM(location) = '';

UPDATE events
SET type = 'INCIDENT'
WHERE type IS NULL OR BTRIM(type) = '';

ALTER TABLE events
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN location SET NOT NULL,
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE events
    ADD CONSTRAINT chk_events_location_not_blank CHECK (BTRIM(location) <> ''),
    ADD CONSTRAINT chk_events_type_not_blank CHECK (BTRIM(type) <> '');
