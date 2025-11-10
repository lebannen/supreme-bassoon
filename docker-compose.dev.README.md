# Docker Compose for Local Development

This project has two Docker Compose configurations:

## `docker-compose.dev.yml` - For Local Development (Recommended)

**Use this when:** Running backend (Spring Boot) and frontend (Vue) locally on your machine.

**What it runs:**
- ✅ PostgreSQL database (port 5432)
- ✅ MinIO storage (ports 9000, 9001)

**What it DOESN'T run:**
- ❌ Backend (you run with `./gradlew bootRun`)
- ❌ Frontend (you run with `npm run dev`)

### Quick Start

```bash
# Start infrastructure services
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down

# Stop and remove volumes (fresh start)
docker-compose -f docker-compose.dev.yml down -v
```

### Access Points

- **PostgreSQL**: `localhost:5432`
  - Database: `vocabee`
  - Username: `vocabee`
  - Password: `vocabee`

- **MinIO Console**: http://localhost:9001
  - Username: `minioadmin`
  - Password: `minioadmin123`

- **MinIO S3 API**: http://localhost:9000
  - Buckets: `vocabee-audio`, `vocabee-files`

### Your Local Development Workflow

```bash
# Terminal 1: Start infrastructure
docker-compose -f docker-compose.dev.yml up -d

# Terminal 2: Start backend
cd backend
./gradlew bootRun

# Terminal 3: Start frontend
cd frontend
npm run dev

# Access your app at http://localhost:5173
```

---

## `docker-compose.yml` - For Production Deployment

**Use this when:** Deploying the full stack to a server.

**What it runs:**
- ✅ PostgreSQL database
- ✅ Backend (built from Dockerfile)
- ✅ Frontend (built from Dockerfile, served via nginx)

### Production Start

```bash
# Build and start all services
docker-compose up -d

# Access app at http://localhost
```

---

## Migrating from Existing PostgreSQL Container

If you already have a `vocabee-postgres` container running:

### Option 1: Use Existing Container (Simplest)
Just don't start the postgres service from docker-compose:

```bash
# Start only MinIO
docker-compose -f docker-compose.dev.yml up -d minio minio-setup
```

### Option 2: Migrate to New Container
```bash
# 1. Dump existing database
docker exec vocabee-postgres pg_dump -U vocabee vocabee > backup.sql

# 2. Stop old container
docker stop vocabee-postgres
docker rm vocabee-postgres

# 3. Start new dev stack
docker-compose -f docker-compose.dev.yml up -d

# 4. Restore database
cat backup.sql | docker exec -i vocabee-postgres-dev psql -U vocabee -d vocabee
```

### Option 3: Keep Both (Different Ports)
Edit `docker-compose.dev.yml` to use different port:
```yaml
postgres:
  ports:
    - "5433:5432"  # Use 5433 instead
```

Then update your backend `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/vocabee
```

---

## Troubleshooting

### Port Already in Use

If port 5432 is taken:
```bash
# Check what's using the port
lsof -i :5432

# Option 1: Stop the existing postgres
docker stop vocabee-postgres

# Option 2: Change port in docker-compose.dev.yml
```

### MinIO Buckets Not Created

If buckets don't exist:
```bash
# Manually run setup
docker-compose -f docker-compose.dev.yml run minio-setup
```

### Reset Everything

```bash
# Stop and remove all containers and volumes
docker-compose -f docker-compose.dev.yml down -v

# Start fresh
docker-compose -f docker-compose.dev.yml up -d
```

---

## Storage Locations

**Volumes (persisted data):**
- `postgres-dev-data` - Database files
- `minio-dev-data` - Uploaded audio/files

**To view volumes:**
```bash
docker volume ls | grep vocabee
```

**To backup MinIO data:**
```bash
docker run --rm -v vocabee_minio-dev-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/minio-backup.tar.gz -C /data .
```
