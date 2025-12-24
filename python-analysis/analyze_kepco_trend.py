import psycopg2
import pandas as pd
import numpy as np
import numpy as np

def analyze_trend():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    try:
        conn = psycopg2.connect(**db_config)
        
        # 1. Get stock code
        cursor = conn.cursor()
        cursor.execute("SELECT code FROM tb_stock_list WHERE name = '한국전력'")
        stk_cd = cursor.fetchone()[0]
        
        # 2. Get data
        query = f"""
            SELECT dt, 
                   orgn_net_buy_amount, 
                   frgnr_invsr_net_buy_amount
            FROM tb_stock_investor_invest_accumulation 
            WHERE stk_cd = '{stk_cd}' 
              AND dt >= '2025-01-01'
            ORDER BY dt ASC
        """
        df = pd.read_sql(query, conn)
        conn.close()
        
        if df.empty:
            print("No data found")
            return

        # Convert cols to float
        df['orgn'] = df['orgn_net_buy_amount'].astype(float)
        df['frgn'] = df['frgnr_invsr_net_buy_amount'].astype(float)
        
        # 3. Anlaysis
        # We want to find when they started "rising together significantly"
        
        # Calculate gradients (daily change is too noisy, let's use a smoothed slope)
        # 20-day moving average of the series to smooth it, then gradient?
        # Or slope of the 20-day regression window.
        
        window = 10
        
        def calculate_slope(series, window):
            slopes = [np.nan] * len(series)
            for i in range(window, len(series)):
                y = series.iloc[i-window:i]
                x = np.arange(window)
                # Linear regression using numpy
                slope, _ = np.polyfit(x, y, 1)
                slopes[i] = slope
            return slopes
            
        df['orgn_slope'] = calculate_slope(df['orgn'], window)
        df['frgn_slope'] = calculate_slope(df['frgn'], window)
        
        # Normalize slopes to compare magnitude or just check sign
        # "Rising significantly" -> Positive slope, and maybe large?
        # We want "simultaneous" rise.
        
        # Let's filter where both slopes are positive
        df['both_rising'] = (df['orgn_slope'] > 0) & (df['frgn_slope'] > 0)
        
        # Find the start of the longest sustained period of both rising, 
        # or the first time they both became significantly positive after being flat/negative.
        
        # Let's look at the Z-score of the slopes to determine "significant"
        df['orgn_slope_z'] = (df['orgn_slope'] - df['orgn_slope'].mean()) / df['orgn_slope'].std()
        df['frgn_slope_z'] = (df['frgn_slope'] - df['frgn_slope'].mean()) / df['frgn_slope'].std()
        
        # Threshold: e.g. Z > 0 (above average) or Z > 0.5
        # The user said "significantly".
        
        df['significant_rise'] = (df['orgn_slope_z'] > 0.0) & (df['frgn_slope_z'] > 0.0) # Lowered to just positive
        
        rising_periods = df[df['significant_rise']]
        
        print(f"Total days: {len(df)}")
        print(f"Slope Org Stats: Mean={df['orgn_slope'].mean():.2f}, Z-Mean={df['orgn_slope_z'].mean():.2f}")
        print(f"Slope Frg Stats: Mean={df['frgn_slope'].mean():.2f}, Z-Mean={df['frgn_slope_z'].mean():.2f}")
        
        print("Analysis for 'Simultaneous Rise' (Both Slopes > Mean):")
        
        if not rising_periods.empty:
            # Group consecutive dates
            rising_periods['group'] = (rising_periods.index.to_series().diff() > 1).cumsum()
            
            groups = rising_periods.groupby('group')
            found_any = False
            for name, group in groups:
                start_date = group['dt'].iloc[0]
                end_date = group['dt'].iloc[-1]
                duration = len(group)
                if duration >= 5: # Keep 5 days
                     print(f"Period: {start_date} ~ {end_date} (Duration: {duration} days)")
                     print(f"  Avg Orgn Slope Z: {group['orgn_slope_z'].mean():.2f}")
                     print(f"  Avg Frgn Slope Z: {group['frgn_slope_z'].mean():.2f}")
                     found_any = True
            if not found_any:
                print("No sustained simultaneous rise > 5 days found (even with Z > 0).")
        else:
            print("No simultaneous rise found (Z > 0).")
            
        # Alternative: Change Point Detection on the sum of normalized accumulation
        # Normalize accumulations
        df['orgn_norm'] = (df['orgn'] - df['orgn'].min()) / (df['orgn'].max() - df['orgn'].min())
        df['frgn_norm'] = (df['frgn'] - df['frgn'].min()) / (df['frgn'].max() - df['frgn'].min())
        df['combined_trend'] = df['orgn_norm'] + df['frgn_norm']
        
        # Find where the 2nd derivative of combined trend is positive (concave up) or 
        # simply where the gradient of combined trend shifts from low to high.
        
        # Let's verify the "unlike the past" part. Use a rolling correlation?
        df['rolling_corr'] = df['orgn'].rolling(window=30).corr(df['frgn'])
        
        print("\nCorrelation Analysis:")
        # Find where correlation becomes highly positive
        high_corr = df[df['rolling_corr'] > 0.8]
        if not high_corr.empty:
             print(f"High Correlation (> 0.8) started appearing around: {high_corr['dt'].iloc[0]}")
             
        # Detect sharp increase point
        # Simple heuristic: Look for date where slopes exploded.
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    analyze_trend()
