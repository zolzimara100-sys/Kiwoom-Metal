---
description: 백엔드 코드 변경 후 Docker 배포 절차
---

# 백엔드 코드 변경 후 Docker 배포 절차

이 프로젝트는 **Docker 컨테이너**에서 실행됩니다. 
백엔드 Java 코드를 수정한 후에는 반드시 다음 절차를 따라야 변경사항이 반영됩니다.

## 중요 사항
- `./gradlew classes`는 컴파일만 수행하며, **배포에는 적용되지 않음**
- Docker 환경에서는 JAR 파일 재빌드 + Docker 이미지 재빌드가 필수

## 배포 절차

// turbo
1. JAR 파일 빌드
```bash
cd /Users/juhyunhwang/kiwoom
./gradlew bootJar
```

// turbo
2. Docker 이미지 재빌드 및 컨테이너 재시작
```bash
docker-compose up --build -d app
```

// turbo
3. 로그 확인 (선택)
```bash
docker-compose logs -f app
```

## 테스트
- API 테스트: `curl http://localhost:8080/api/statistics/moving-average/chart/015760?period=30`
- 프론트엔드: `http://localhost:3000/moving-chart?stkCd=015760`

## 문제 해결
- 변경사항이 반영되지 않으면 Docker 캐시 삭제 후 재빌드:
```bash
docker-compose down
docker-compose build --no-cache app
docker-compose up -d
```
