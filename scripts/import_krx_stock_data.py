#!/usr/bin/env python3
"""
KRX 종목 마스터 데이터 PostgreSQL 삽입 프로그램

한국거래소 상장종목 정보와 KSIC 산업분류 정보를 다운로드하여
PostgreSQL tb_stock_list_krx 테이블에 삽입하는 프로그램
"""

import pandas as pd
import psycopg2
from psycopg2.extras import execute_batch
import requests
import gzip
import io
import logging
from typing import Optional
import os
from datetime import datetime

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('krx_data_import.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class KRXStockDataImporter:
    """KRX 종목 데이터 임포터"""
    
    def __init__(self, db_config: dict):
        """
        Args:
            db_config: 데이터베이스 연결 설정
                - host, port, database, user, password
        """
        self.db_config = db_config
        self.data_url = "https://github.com/FinanceData/stock_master/raw/master/stock_master.csv.gz"
        
    def download_data(self) -> pd.DataFrame:
        """KRX 종목 마스터 데이터 다운로드"""
        logger.info("KRX 종목 마스터 데이터 다운로드 시작")
        
        try:
            response = requests.get(self.data_url, timeout=30)
            response.raise_for_status()
            
            # gzip 압축 해제
            with gzip.GzipFile(fileobj=io.BytesIO(response.content)) as gz_file:
                df = pd.read_csv(
                    gz_file, 
                    dtype={
                        'Symbol': str, 
                        'Industy_code': str,  # 원본 오타 그대로 사용
                        'Name': str,
                        'Market': str,
                        'Industry': str,
                        'Sector': str,
                        'Industy_name': str
                    }
                )
            
            logger.info(f"데이터 다운로드 완료: {len(df):,}개 종목")
            return df
            
        except Exception as e:
            logger.error(f"데이터 다운로드 실패: {e}")
            raise
    
    def preprocess_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """데이터 전처리"""
        logger.info("데이터 전처리 시작")
        
        # 컬럼명 정리 (원본 오타 수정)
        df = df.rename(columns={
            'Symbol': 'symbol',
            'Name': 'name', 
            'Market': 'market',
            'Listing': 'listing',
            'Industry': 'industry',
            'Sector': 'sector',
            'Industy_code': 'industry_code',  # 오타 수정
            'Industy_name': 'industry_name'   # 오타 수정
        })
        
        # 데이터 타입 확인 및 정리
        df['symbol'] = df['symbol'].astype(str).str.zfill(6)  # 6자리 종목코드 보장
        df['listing'] = df['listing'].astype(bool)
        
        # 문자열 필드 길이 제한 (PostgreSQL 컬럼 크기에 맞춤)
        df['name'] = df['name'].astype(str).str[:100]
        df['market'] = df['market'].astype(str).str[:10]
        df['industry'] = df['industry'].astype(str).str[:500]
        df['sector'] = df['sector'].astype(str).str[:100]
        df['industry_code'] = df['industry_code'].astype(str).str[:10]
        df['industry_name'] = df['industry_name'].astype(str).str[:200]
        
        # NULL 값을 None으로 변경 (PostgreSQL에서 NULL로 처리)
        df = df.where(pd.notnull(df), None)
        
        # 중복 종목코드 확인
        duplicates = df[df['symbol'].duplicated()]
        if not duplicates.empty:
            logger.warning(f"중복 종목코드 발견: {len(duplicates)}개")
            logger.warning(f"중복 종목: {duplicates['symbol'].tolist()}")
            # 첫 번째 항목만 유지
            df = df.drop_duplicates(subset=['symbol'], keep='first')
        
        logger.info(f"데이터 전처리 완료: {len(df):,}개 종목")
        return df
    
    def create_connection(self) -> psycopg2.extensions.connection:
        """PostgreSQL 연결 생성"""
        try:
            conn = psycopg2.connect(**self.db_config)
            conn.autocommit = False  # 트랜잭션 모드
            logger.info("PostgreSQL 연결 성공")
            return conn
        except Exception as e:
            logger.error(f"PostgreSQL 연결 실패: {e}")
            raise
    
    def create_table(self, conn: psycopg2.extensions.connection):
        """테이블 생성"""
        create_table_sql = """
        -- KRX 종목 마스터 테이블 생성
        DROP TABLE IF EXISTS tb_stock_list_krx;

        CREATE TABLE tb_stock_list_krx (
            symbol VARCHAR(10) NOT NULL,                    -- 종목코드 (6자리, PK)
            name VARCHAR(100) NOT NULL,                     -- 종목명
            market VARCHAR(10) NOT NULL,                    -- 시장구분 (KOSPI/KOSDAQ/KONEX)
            listing BOOLEAN NOT NULL DEFAULT true,         -- 상장여부 (true: 상장, false: 상장폐지)
            industry VARCHAR(500),                          -- 거래소 산업구분 (사업내용)
            sector VARCHAR(100),                            -- 거래소 섹터 구분
            industry_code VARCHAR(10),                      -- DART 산업분류 코드 (KSIC)
            industry_name VARCHAR(200),                     -- DART 산업 이름
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 생성일시
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 수정일시
            
            CONSTRAINT pk_stock_list_krx PRIMARY KEY (symbol)
        );

        -- 인덱스 생성
        CREATE INDEX idx_stock_list_krx_market ON tb_stock_list_krx(market);
        CREATE INDEX idx_stock_list_krx_listing ON tb_stock_list_krx(listing);
        CREATE INDEX idx_stock_list_krx_sector ON tb_stock_list_krx(sector);
        CREATE INDEX idx_stock_list_krx_industry_code ON tb_stock_list_krx(industry_code);

        -- 테이블 코멘트 추가
        COMMENT ON TABLE tb_stock_list_krx IS 'KRX 종목 마스터 - 한국거래소 상장종목 및 KSIC 산업분류 정보';
        COMMENT ON COLUMN tb_stock_list_krx.symbol IS '종목코드 (6자리)';
        COMMENT ON COLUMN tb_stock_list_krx.name IS '종목명';
        COMMENT ON COLUMN tb_stock_list_krx.market IS '시장구분 (KOSPI/KOSDAQ/KONEX)';
        COMMENT ON COLUMN tb_stock_list_krx.listing IS '상장여부 (true: 상장, false: 상장폐지)';
        COMMENT ON COLUMN tb_stock_list_krx.industry IS '거래소 산업구분 (회사 사업내용)';
        COMMENT ON COLUMN tb_stock_list_krx.sector IS '거래소 섹터 구분';
        COMMENT ON COLUMN tb_stock_list_krx.industry_code IS 'DART 산업분류 코드 (KSIC)';
        COMMENT ON COLUMN tb_stock_list_krx.industry_name IS 'DART 산업 이름';
        """
        
        try:
            with conn.cursor() as cur:
                cur.execute(create_table_sql)
            conn.commit()
            logger.info("테이블 생성/재생성 완료")
        except Exception as e:
            logger.error(f"테이블 생성 실패: {e}")
            conn.rollback()
            raise
    
    def insert_data(self, conn: psycopg2.extensions.connection, df: pd.DataFrame):
        """데이터 삽입"""
        logger.info(f"데이터 삽입 시작: {len(df):,}개 레코드")
        
        insert_sql = """
        INSERT INTO tb_stock_list_krx (
            symbol, name, market, listing, industry, sector, 
            industry_code, industry_name, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, 
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) ON CONFLICT (symbol) DO UPDATE SET
            name = EXCLUDED.name,
            market = EXCLUDED.market,
            listing = EXCLUDED.listing,
            industry = EXCLUDED.industry,
            sector = EXCLUDED.sector,
            industry_code = EXCLUDED.industry_code,
            industry_name = EXCLUDED.industry_name,
            updated_at = CURRENT_TIMESTAMP
        """
        
        try:
            with conn.cursor() as cur:
                # 데이터를 튜플 리스트로 변환
                data_tuples = [
                    (
                        row['symbol'], row['name'], row['market'], row['listing'],
                        row['industry'], row['sector'], row['industry_code'], row['industry_name']
                    )
                    for _, row in df.iterrows()
                ]
                
                # 배치 삽입 (성능 향상)
                execute_batch(cur, insert_sql, data_tuples, page_size=1000)
                
            conn.commit()
            logger.info(f"데이터 삽입 완료: {len(df):,}개 레코드")
            
        except Exception as e:
            logger.error(f"데이터 삽입 실패: {e}")
            conn.rollback()
            raise
    
    def verify_data(self, conn: psycopg2.extensions.connection) -> dict:
        """데이터 검증"""
        logger.info("데이터 검증 시작")
        
        queries = {
            'total_count': "SELECT COUNT(*) FROM tb_stock_list_krx",
            'market_dist': """
                SELECT market, COUNT(*) as count 
                FROM tb_stock_list_krx 
                GROUP BY market ORDER BY count DESC
            """,
            'listing_dist': """
                SELECT listing, COUNT(*) as count 
                FROM tb_stock_list_krx 
                GROUP BY listing
            """,
            'null_counts': """
                SELECT 
                    COUNT(*) as total,
                    COUNT(industry) as has_industry,
                    COUNT(sector) as has_sector,
                    COUNT(industry_code) as has_industry_code,
                    COUNT(industry_name) as has_industry_name
                FROM tb_stock_list_krx
            """
        }
        
        results = {}
        
        try:
            with conn.cursor() as cur:
                for key, query in queries.items():
                    cur.execute(query)
                    results[key] = cur.fetchall()
            
            # 결과 출력
            logger.info("=== 데이터 검증 결과 ===")
            logger.info(f"총 레코드 수: {results['total_count'][0][0]:,}")
            
            logger.info("시장 분포:")
            for market, count in results['market_dist']:
                logger.info(f"  {market}: {count:,}")
            
            logger.info("상장여부 분포:")
            for listing, count in results['listing_dist']:
                status = "상장" if listing else "상장폐지"
                logger.info(f"  {status}: {count:,}")
            
            total, has_industry, has_sector, has_code, has_name = results['null_counts'][0]
            logger.info("데이터 완성도:")
            logger.info(f"  Industry: {has_industry:,}/{total:,} ({has_industry/total*100:.1f}%)")
            logger.info(f"  Sector: {has_sector:,}/{total:,} ({has_sector/total*100:.1f}%)")
            logger.info(f"  Industry Code: {has_code:,}/{total:,} ({has_code/total*100:.1f}%)")
            logger.info(f"  Industry Name: {has_name:,}/{total:,} ({has_name/total*100:.1f}%)")
            
            return results
            
        except Exception as e:
            logger.error(f"데이터 검증 실패: {e}")
            raise
    
    def run(self):
        """전체 프로세스 실행"""
        start_time = datetime.now()
        logger.info(f"KRX 종목 마스터 데이터 임포트 시작: {start_time}")
        
        try:
            # 1. 데이터 다운로드
            df = self.download_data()
            
            # 2. 데이터 전처리
            df = self.preprocess_data(df)
            
            # 3. 데이터베이스 연결
            conn = self.create_connection()
            
            try:
                # 4. 테이블 생성
                self.create_table(conn)
                
                # 5. 데이터 삽입
                self.insert_data(conn, df)
                
                # 6. 데이터 검증
                self.verify_data(conn)
                
            finally:
                conn.close()
                logger.info("데이터베이스 연결 종료")
            
            end_time = datetime.now()
            elapsed = end_time - start_time
            logger.info(f"전체 프로세스 완료: {elapsed}")
            
        except Exception as e:
            logger.error(f"프로세스 실행 중 오류: {e}")
            raise

def main():
    """메인 함수"""
    
    # 데이터베이스 설정 (환경변수 또는 직접 설정)
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': os.getenv('DB_PORT', '5432'),
        'database': os.getenv('DB_NAME', 'kiwoom'),
        'user': os.getenv('DB_USER', 'kiwoom'),
        'password': os.getenv('DB_PASSWORD', 'kiwoom123')
    }
    
    # 임포터 실행
    importer = KRXStockDataImporter(db_config)
    importer.run()

if __name__ == "__main__":
    main()