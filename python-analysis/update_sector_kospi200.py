import pandas as pd
import psycopg2
import os

def update_sector_data():
    csv_file = "classified_stock_list.csv"
    
    if not os.path.exists(csv_file):
        print(f"Error: {csv_file} not found.")
        return

    # DB Config
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
        print("Connected to PostgreSQL.")

        # 1. Add Column if not exists
        print("Ensuring 'sector' column exists...")
        cursor.execute("ALTER TABLE tb_stock_list_meta ADD COLUMN IF NOT EXISTS sector VARCHAR(255);")
        conn.commit()

        # 2. Read CSV
        df = pd.read_csv(csv_file, encoding='utf-8-sig')
        # Fill NaN with empty string just in case
        df['categories'] = df['categories'].fillna('')
        
        # 3. Update Data
        print(f"Updating sectors for {len(df)} rows...")
        
        update_query = "UPDATE tb_stock_list_meta SET sector = %s WHERE code = %s"
        
        updated_count = 0
        
        for index, row in df.iterrows():
            code = str(row['code']).zfill(6) # Ensure 6 digits
            sector = row['categories']
            
            # Optimization: Update only if sector is not empty, OR update everything?
            # User said "update tb_stock_list_meta.sector = categories".
            # If categories is empty, we set it to empty string or NULL?
            # Let's set it to valid string.
            
            cursor.execute(update_query, (sector, code))
            updated_count += 1
            
        conn.commit()
        print(f"Successfully updated {updated_count} records.")

        cursor.close()
        conn.close()

    except Exception as e:
        print(f"Error: {e}")
        if 'conn' in locals() and conn:
            conn.rollback()

if __name__ == "__main__":
    update_sector_data()
