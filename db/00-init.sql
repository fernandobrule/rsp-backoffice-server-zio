-- Executed once by the postgres image entrypoint, before the DDL (01-...).
-- The DDL uses geography(point, 4326), so PostGIS is required.
create schema if not exists rsp_db_schema_dev;
create extension if not exists postgis;
