import psycopg2
import os

def run_migration():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    sql_file = 'create_ma_table_full.sql'
    
    try:
        # Read the SQL file
        with open(sql_file, 'r') as f:
            sql_script = f.read()
            
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()
        
        print(f"Executing SQL from {sql_file}...")
        cursor.execute(sql_script)
        conn.commit()
        
        print("Table 'tb_stock_investor_ma' and indexes created successfully.")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error during migration: {e}")

if __name__ == "__main__":
    run_migration()
