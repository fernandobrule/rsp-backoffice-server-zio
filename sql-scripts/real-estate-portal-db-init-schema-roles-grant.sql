
--
-- schema
--
create schema if not exists rsp_db_schema_dev;

--
-- roles
--

-- login user for the application
create role rsp_db_user_dev with login password 'S8gUlJuf#ahNp';

--
-- grants
--

-- give the user the group's permissions
grant rsp_db_role_dev to rsp_db_user_dev;

-- grant connect to the database
grant connect on database rsp_db_dev to rsp_db_role_dev;

-- grant access to the schema
grant usage on schema rsp_db_schema_dev to rsp_db_role_dev;

-- grant existing tables
grant select, insert, update, delete on all tables in schema rsp_db_schema_dev to rsp_db_role_dev;
-- grant existing sequences
grant usage, select on all sequences in schema rsp_db_schema_dev to rsp_db_role_dev;


--
-- alter
--

-- future tables and sequences (created by the role running this command)
alter default privileges in schema rsp_db_schema_dev grant select, insert, update, delete on tables to rsp_db_role_dev;
alter default privileges in schema rsp_db_schema_dev grant usage, select on sequences to rsp_db_role_dev;

