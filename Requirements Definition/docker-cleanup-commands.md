# Docker 리소스 정리 가이드

## 현재 상태 (2025-12-24)
- Dangling volumes: 27개 (~4GB)
- Dangling images: 94개 (~15GB)
- Build cache: 31.84GB
- **총 회수 가능 용량: 약 50GB**

## 안전한 정리 명령어

### 1. Dangling 볼륨 정리 (추천)
```bash
# 미리보기
docker volume ls -qf dangling=true

# 삭제
docker volume prune -f
```

### 2. Dangling 이미지 정리 (추천)
```bash
# 미리보기
docker images -f "dangling=true"

# 삭제
docker image prune -f
```

### 3. 빌드 캐시 정리 (선택)
```bash
# 미리보기
docker buildx du

# 오래된 캐시만 삭제
docker builder prune -f

# 모든 캐시 삭제 (빌드가 느려질 수 있음)
docker builder prune -af
```

### 4. 전체 한번에 정리 (가장 추천)
```bash
# 사용하지 않는 모든 리소스 정리
docker system prune -af --volumes

# 단, 실행중인 컨테이너와 연결된 리소스는 보존됨
```

## 주의사항
- ✅ 실행 중인 컨테이너의 볼륨/이미지는 자동으로 보호됨
- ✅ kiwoom_postgres-data, kiwoom_redis-data는 현재 사용 중이므로 삭제되지 않음
- ⚠️ 중지된 컨테이너의 볼륨도 삭제될 수 있으니 확인 필요

## 정리 후 예상 효과
- 약 50GB 디스크 공간 확보
- Docker 성능 향상
- 리소스 목록이 깔끔해짐
