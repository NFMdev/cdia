-- V5__event_type_column.sql
-- Add event type to support anomaly type propagation from simulator to map filters

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS type VARCHAR(50);
