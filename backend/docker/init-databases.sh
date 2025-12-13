#!/bin/bash

# This script is used by PostgreSQL Docker container to create multiple databases
# It runs on first startup only (when the data volume is empty)

set -e
set -u

function create_database() {
    local database=$1
    echo "Creating database '$database'..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'Database $database already exists' WHERE EXISTS (SELECT FROM pg_database WHERE datname = '$database')
        \gset
        CREATE DATABASE $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
    echo "Database '$database' created successfully!"
}

# Check if POSTGRES_MULTIPLE_DATABASES is set
if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Multiple databases creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        # Skip if database already exists or is the default one
        if [ "$db" != "$POSTGRES_DB" ]; then
            create_database "$db"
        fi
    done
    echo "All databases created successfully!"
fi
