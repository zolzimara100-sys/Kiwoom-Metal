# KOSPI200 to PostgreSQL Insert

## 개요
KOSPI200 종목 리스트를 CSV 파일에서 읽어 PostgreSQL 데이터베이스에 삽입하는 프로그램입니다.

## 기능
- `kospi200_list.csv` 파일 읽기
- 종목코드를 6자리로 패딩 (예: "5930" → "005930")
- PostgreSQL 데이터베이스에 INSERT/UPDATE

## 컬럼 매핑
| CSV 컬럼 | DB 컬럼 | 설명 |
|---------|--------|------|
| code    | code   | 종목코드 (6자리로 패딩) |
| name    | name   | 종목명 |
| market  | main   | 시장 구분 (KOSPI200) |

## 사전 준비

### 1. 패키지 설치
```bash
pip install psycopg2-binary pandas
```

### 2. PostgreSQL 테이블 생성
```sql
CREATE TABLE stocks (
    code VARCHAR(6) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    main VARCHAR(50)
);
```

## 사용 방법

### 1. DB 연결 정보 수정
`insert_to_postgres.py` 파일에서 DB 연결 정보를 수정하세요:

```python
db_config = {
    'host': 'localhost',        # DB 호스트
    'port': 5432,               # DB 포트
    'database': 'your_database', # 데이터베이스 이름
    'user': 'your_username',     # 사용자 이름
    'password': 'your_password'  # 비밀번호
}
```

### 2. 프로그램 실행
```bash
cd /Users/juhyunhwang/kiwoom/python-analysis/crawler-kospi200
source ../venv/bin/activate
python insert_to_postgres.py
```

## 출력 예시
```
============================================================
KOSPI200 종목 데이터 PostgreSQL INSERT 프로그램
============================================================
CSV 파일에서 200개의 레코드를 읽었습니다.

[상위 5개 레코드 미리보기 (패딩 후)]
    code      name   market
0  005930      삼성전자 KOSPI200
1  000660    SK하이닉스 KOSPI200
2  373220  LG에너지솔루션 KOSPI200
3  207940  삼성바이오로직스 KOSPI200
4  005380       현대차 KOSPI200

PostgreSQL 데이터베이스에 연결되었습니다.

총 200개의 레코드가 성공적으로 삽입/업데이트되었습니다.
데이터베이스 연결을 종료했습니다.
```

## 특징
- **중복 처리**: `ON CONFLICT` 절을 사용하여 동일한 code가 있으면 UPDATE
- **에러 핸들링**: 각 레코드 삽입 실패 시 에러 메시지 출력 후 계속 진행
- **트랜잭션**: 모든 작업 완료 후 COMMIT

## 파일 구조
```
crawler-kospi200/
├── main_crawler_kospi200.py    # KOSPI200 크롤러 실행 파일
├── kospi200_crawler.py          # 크롤러 클래스
├── stock_dto.py                 # DTO 클래스
├── kospi200_list.csv            # 크롤링 결과 CSV
├── insert_to_postgres.py        # PostgreSQL INSERT 스크립트
└── README.md                    # 이 파일
```

## 주의사항
1. PostgreSQL 서버가 실행 중이어야 합니다
2. DB 연결 정보를 정확히 입력해야 합니다
3. stocks 테이블이 미리 생성되어 있어야 합니다
4. code 컬럼은 PRIMARY KEY로 설정되어야 합니다
