\set ON_ERROR_STOP 1
\getenv migrations_user CDIA_MIGRATIONS_DB_USER

\if :{?migrations_user}
\else
\set migrations_user cdia_migrations
\endif

-- Grant replication privilege used by Flink CDC.
SELECT format('ALTER ROLE %I WITH REPLICATION', :'migrations_user')
WHERE EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'migrations_user'
)\gexec
