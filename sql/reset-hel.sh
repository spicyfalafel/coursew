#!/bin/bash

psql -h pg -d studs -f drop.sql
psql -h pg -d studs -f create.sql
psql -h pg -d studs -f indexes.sql
psql -h pg -d studs -f triggers.sql
psql -h pg -d studs -f indexes.sql
psql -h pg -d studs -f insert.sql
