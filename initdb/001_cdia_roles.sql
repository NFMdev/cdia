\set ON_ERROR_STOP 1

-- Pull role credentials from environment variables provided to the Postgres container.
\getenv app_user CDIA_APP_DB_USER
\getenv app_password CDIA_APP_DB_PASSWORD
\getenv migrations_user CDIA_MIGRATIONS_DB_USER
\getenv migrations_password CDIA_MIGRATIONS_DB_PASSWORD
\getenv metrics_user CDIA_METRICS_DB_USER
\getenv metrics_password CDIA_METRICS_DB_PASSWORD

\if :{?app_user}
\else
\set app_user cdia_app
\endif
\if :{?app_password}
\else
\set app_password change_me_app
\endif
\if :{?migrations_user}
\else
\set migrations_user cdia_migrations
\endif
\if :{?migrations_password}
\else
\set migrations_password change_me_migrations
\endif
\if :{?metrics_user}
\else
\set metrics_user cdia_metrics
\endif
\if :{?metrics_password}
\else
\set metrics_password change_me_metrics
\endif

-- Create roles idempotently.
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'app_user'
)\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'migrations_user', :'migrations_password')
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'migrations_user'
)\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'metrics_user', :'metrics_password')
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'metrics_user'
)\gexec

-- Keep credentials aligned with env values on repeated init runs.
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'migrations_user', :'migrations_password') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'metrics_user', :'metrics_password') \gexec

-- Grant database connectivity.
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'migrations_user') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'metrics_user') \gexec
SELECT format('GRANT CREATE ON DATABASE %I TO %I', current_database(), :'migrations_user') \gexec

-- Migration role owns schema and manages DDL; application role gets DML-only access.
SELECT format('ALTER SCHEMA public OWNER TO %I', :'migrations_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
SELECT format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', :'app_user') \gexec
SELECT format('GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO %I', :'app_user') \gexec

-- Ensure future objects created by migration role are automatically available to app role.
SELECT format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
    :'migrations_user',
    :'app_user'
) \gexec
SELECT format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I',
    :'migrations_user',
    :'app_user'
) \gexec

-- Monitoring role for postgres_exporter.
SELECT format('GRANT pg_monitor TO %I', :'metrics_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'metrics_user') \gexec
