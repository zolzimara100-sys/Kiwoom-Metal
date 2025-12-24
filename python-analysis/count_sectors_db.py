import psycopg2
from collections import Counter
import pandas as pd

def count_db_sectors():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    try:
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()
        
        # Select all sectors
        cursor.execute("SELECT sector FROM tb_stock_list_meta")
        rows = cursor.fetchall()
        
        all_tags = []
        
        for row in rows:
            # row[0] might be "반도체,AI/인공지능"
            if row[0]:
                tags = row[0].split(',')
                # Trim whitespace just in case
                tags = [tag.strip() for tag in tags if tag.strip()]
                all_tags.extend(tags)
                
        # Count
        counts = Counter(all_tags)
        
        # Sort by count desc
        sorted_counts = counts.most_common()
        
        print(f"\n[tb_stock_list_meta 섹터별 종목 수 집계]")
        print(f"총 고유 섹터 개수: {len(sorted_counts)}")
        print("-" * 40)
        print(f"{'섹터명':<20} | {'종목 수':>10}")
        print("-" * 40)
        
        for sector, count in sorted_counts:
            # 한글 정렬을 위해 ljust 등 사용 시 폭 계산이 어렵지만 단순 출력
            print(f"{sector:<20} | {count:>10}")
            
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    count_db_sectors()
