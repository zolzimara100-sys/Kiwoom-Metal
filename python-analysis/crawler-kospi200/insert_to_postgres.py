import pandas as pd
import psycopg2
from psycopg2 import sql
import os


def pad_code(code):
    """종목코드를 6자리로 맞춥니다. 부족한 경우 앞에 0을 추가합니다."""
    code_str = str(code)
    return code_str.zfill(6)


def insert_kospi200_to_db():
    """
    kospi200_list.csv 파일을 읽어서 PostgreSQL DB에 insert합니다.
    
    컬럼 매핑:
    - CSV의 code -> DB의 code (6자리로 패딩)
    - CSV의 name -> DB의 name
    - CSV의 market -> DB의 main
    """
    
    # 1. CSV 파일 읽기
    csv_file = "kospi200_list.csv"
    
    if not os.path.exists(csv_file):
        print(f"오류: {csv_file} 파일을 찾을 수 없습니다.")
        return
    
    df = pd.read_csv(csv_file, encoding='utf-8-sig')
    print(f"CSV 파일에서 {len(df)}개의 레코드를 읽었습니다.")
    
    # 2. code를 6자리로 패딩
    df['code'] = df['code'].apply(pad_code)
    
    # 3. 데이터 미리보기
    print("\n[상위 5개 레코드 미리보기 (패딩 후)]")
    print(df[['code', 'name', 'market']].head())
    
    # 4. PostgreSQL 연결 설정
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }
    
    try:
        # 5. DB 연결
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()
        
        print("\nPostgreSQL 데이터베이스에 연결되었습니다.")
        
        # 6. 기존 데이터 삭제 (KOSPI200만)
        delete_query = "DELETE FROM tb_stock_list_meta WHERE main = 'KOSPI200';"
        cursor.execute(delete_query)
        print(f"기존 KOSPI200 데이터를 삭제했습니다.")
        
        # 7. INSERT 쿼리 실행
        insert_query = """
            INSERT INTO tb_stock_list_meta (code, name, main)
            VALUES (%s, %s, %s);
        """
        
        # 8. 데이터 삽입
        inserted_count = 0
        for idx, row in df.iterrows():
            try:
                cursor.execute(insert_query, (
                    row['code'],
                    row['name'],
                    row['market']
                ))
                inserted_count += 1
            except Exception as e:
                print(f"레코드 삽입 실패 (code={row['code']}): {e}")
                conn.rollback()
                raise
        
        # 9. 커밋
        conn.commit()
        print(f"\n총 {inserted_count}개의 레코드가 성공적으로 삽입되었습니다.")
        
        # 10. 연결 종료
        cursor.close()
        conn.close()
        print("데이터베이스 연결을 종료했습니다.")
        
    except psycopg2.Error as e:
        print(f"\nDB 연결 또는 작업 중 오류 발생: {e}")
        if 'conn' in locals():
            conn.rollback()
    except Exception as e:
        print(f"\n예기치 않은 오류 발생: {e}")


if __name__ == "__main__":
    print("=" * 60)
    print("KOSPI200 종목 데이터 PostgreSQL INSERT 프로그램")
    print("=" * 60)
    
    insert_kospi200_to_db()
