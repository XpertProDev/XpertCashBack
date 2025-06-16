#!/bin/bash

echo "🔁 Build Maven JAR..."
./mvnw clean package -DskipTests

echo "🐳 Docker build..."
docker-compose build springboot-app

echo "🚀 Restart Docker containers..."
docker-compose up -d

echo "✅ Done. Application is running on http://localhost:8080"
