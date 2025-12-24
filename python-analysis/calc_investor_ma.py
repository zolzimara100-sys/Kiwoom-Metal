import psycopg2
import time

def generate_ma_query():
    # Source column name, Target prefix
    investors = [
        ('frgnr_invsr', 'frgnr_invsr'),
        ('orgn', 'orgn'),
        ('fnnc_invt', 'fnnc_invt'),
        ('insrnc', 'insrnc'),
        ('invtrt', 'invtrt'),
        ('etc_fnnc', 'etc_fnnc'),
        ('bank', 'bank'),
        ('penfnd_etc', 'penfnd_etc'),
        ('samo_fund', 'samo_fund'),
        ('natn', 'natn'),
        ('etc_corp', 'etc_corp'),
        ('natfor', 'natfor')
    ]
    
    periods = [5, 10, 20, 60]
    
    select_clauses = []
    
    # Select clauses
    select_clauses.append("c.stk_cd")
    # Cast date to string and remove hyphens if necessary, or just cast to varchar(8) if format is YYYYMMDD
    # Based on debug, source dt is DATE type (e.g. 2025-12-04), target is VARCHAR(8)
    select_clauses.append("TO_CHAR(c.dt, 'YYYYMMDD') as dt") 
    select_clauses.append("m.sector")
    select_clauses.append("m.main AS category1")
    select_clauses.append("m.sub AS category2")
    select_clauses.append("m.detail AS category3")
    
    # Generate Window Functions for each investor and period
    for src_col, target_prefix in investors:
        for period in periods:
            # -1 because current row is included
            window_size = period - 1
            clause = f"ROUND(AVG(c.{src_col}) OVER (PARTITION BY c.stk_cd ORDER BY c.dt ROWS BETWEEN {window_size} PRECEDING AND CURRENT ROW), 2) AS {target_prefix}_ma{period}"
            select_clauses.append(clause)
            
    query = f"""
    INSERT INTO tb_stock_investor_ma (
        stk_cd, dt, sector, category1, category2, category3,
        {', '.join([f'{target}_ma{p}' for _, target in investors for p in periods])}
    )
    SELECT
        {', '.join(select_clauses)}
    FROM tb_stock_investor_chart c
    LEFT JOIN tb_stock_list_meta m ON c.stk_cd = m.code
    """
    
    return query

def main():
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
        
        start_time = time.time()
        
        print("1. Truncating target table 'tb_stock_investor_ma'...")
        cursor.execute("TRUNCATE TABLE tb_stock_investor_ma")
        
        print("2. Generating and executing INSERT query...")
        insert_sql = generate_ma_query()
        
        # print("--- Query Preview ---")
        # print(insert_sql[:500] + " ...")
        # print("---------------------")
        
        cursor.execute(insert_sql)
        row_count = cursor.rowcount
        conn.commit()
        
        elapsed = time.time() - start_time
        print(f"Success! {row_count} rows inserted in {elapsed:.2f} seconds.")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
