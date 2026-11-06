
set search_path to rsp_db_schema_main_dev;

select current_database(), current_schema();

select extname, extversion, extnamespace::regnamespace
from pg_extension
where extname = 'postgis';

select n.nspname, t.typname
from pg_type t
join pg_namespace n on n.oid = t.typnamespace
where t.typname = 'geography';

