#!/bin/bash

dataBaseName=rsp_db_dev
dataBaseUser=rsp_db_role_dev
containerName=rsp_postgres_db_dev

docker exec -i $containerName \
  psql -U $dataBaseUser \
  -d $dataBaseName < ./sql-scripts/real-estate-portal-db-init-schema-roles-grant.sql
