\set ON_ERROR_STOP 1

-- Run this script with a superuser connection (for example: postgres).
-- It is idempotent and safe to execute multiple times.

-- 1) Create local application role if it does not exist
SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L',
    'admin',
    'admin'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'admin'
)\gexec

-- Keep local credentials/privileges aligned with application defaults
ALTER ROLE admin WITH LOGIN PASSWORD 'admin';
ALTER ROLE admin WITH REPLICATION;

-- 2) Create application database if it does not exist
SELECT format(
    'CREATE DATABASE %I OWNER %I',
    'crime_analytics',
    'admin'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_database WHERE datname = 'crime_analytics'
)\gexec

-- Ensure ownership/privileges are correct if DB already existed
SELECT 'ALTER DATABASE crime_analytics OWNER TO admin'
WHERE EXISTS (
    SELECT 1 FROM pg_catalog.pg_database WHERE datname = 'crime_analytics'
)\gexec

GRANT ALL PRIVILEGES ON DATABASE crime_analytics TO admin;

-- 3) Ensure schema permissions inside the target DB
\connect crime_analytics
ALTER SCHEMA public OWNER TO admin;
GRANT ALL ON SCHEMA public TO admin;
