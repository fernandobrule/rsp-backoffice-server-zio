#!/bin/bash

echo ""
echo "> docker delete al containers"
docker kill $(docker ps -q)
docker rm $(docker ps -a -q)

echo ""
echo "> docker delete al images"
docker rmi $(docker images -q)

echo ""
echo "> docker delete al volumes"
docker volume rm $(docker volume ls -q)

echo ""
echo "> docker compose up"
docker compose up -d

echo ""