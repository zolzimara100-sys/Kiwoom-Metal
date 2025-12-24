# Docker Compose 사용 가이드

## 서비스 구성
- **PostgreSQL**: 포트 5432
- **Redis**: 포트 6379
- **Kiwoom App**: 포트 8080

## 사용 방법

### 1. 전체 서비스 시작
```bash
docker-compose up -d
```

### 2. 특정 서비스만 시작
```bash
# PostgreSQL만 시작
docker-compose up -d postgres

# Redis만 시작
docker-compose up -d redis

# 앱만 시작
docker-compose up -d app
```

### 3. 서비스 상태 확인
```bash
docker-compose ps
```

### 4. 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

### 5. 서비스 중지
```bash
# 중지 (컨테이너 유지)
docker-compose stop

# 중지 및 삭제
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

### 6. 서비스 재시작
```bash
docker-compose restart
```

## 데이터베이스 접속 정보

### PostgreSQL
- Host: localhost
- Port: 5432
- Database: kiwoom
- Username: kiwoom
- Password: kiwoom123

### Redis
- Host: localhost
- Port: 6379

## 컨테이너 내부 접속

### PostgreSQL 접속
```bash
docker exec -it kiwoom-postgres psql -U kiwoom -d kiwoom
```

### Redis 접속
```bash
docker exec -it kiwoom-redis redis-cli
```

## 데이터 볼륨
- PostgreSQL 데이터: `postgres-data` 볼륨
- Redis 데이터: `redis-data` 볼륨
