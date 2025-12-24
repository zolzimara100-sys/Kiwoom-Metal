import psycopg2
import sys

def delete_dates():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    dates_to_delete = ['2025-12-10', '2025-12-09']

    try:
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()
        
        print(f"Connecting to database and deleting records for: {dates_to_delete}")
        
        # Construct SQL with placeholders
        format_strings = ','.join(['%s'] * len(dates_to_delete))
        query = f"DELETE FROM tb_stock_investor_chart WHERE dt IN ({format_strings})"
        
        cursor.execute(query, tuple(dates_to_delete))
        row_count = cursor.rowcount
        
        conn.commit()
        print(f"[Success] Deleted {row_count} rows from tb_stock_investor_chart.")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"[Error] Failed to delete records: {e}")
        sys.exit(1)

if __name__ == "__main__":
    delete_dates()
