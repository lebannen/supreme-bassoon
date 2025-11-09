#!/bin/bash

# Script to reset database and run fresh migrations
# WARNING: This will DELETE ALL DATA!

set -e  # Exit on error

echo "🗑️  Resetting database..."

# Execute the reset SQL script
PGPASSWORD=vocabee psql -h localhost -U vocabee -d vocabee -f reset_database.sql

echo "✅ Database reset complete!"
echo ""
echo "📋 Running fresh migrations..."

# Run Gradle to execute migrations
cd /Users/andrii/Projects/vocabee/backend
./gradlew flywayMigrate

echo ""
echo "✅ Migrations complete!"
echo ""
echo "📊 Checking database tables..."

# List all tables
PGPASSWORD=vocabee psql -h localhost -U vocabee -d vocabee -c "\dt"

echo ""
echo "🎉 Database is ready!"
