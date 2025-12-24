
import pandas as pd
import psycopg2
import numpy as np
from datetime import timedelta

def get_db_connection():
    return psycopg2.connect(
        host="localhost",
        database="kiwoom",
        user="kiwoom",
        password="kiwoom123",
        port="5432"
    )

def detect_change_points(signal, threshold=2.0, min_dist=10):
    """
    Detects change points in the mean of the signal using a sliding window approach
    or simple Z-score on differenced series to find structural breaks.
    Here we use a simplified approach: specific events where the trend (slope) changes significantly.
    
    Target: Series of accumulated amount.
    Derivative: Daily Net Buying (Slope).
    
    We look for structural changes in the Daily Net Buying pattern.
    """
    
    # 1. Calculate Daily Net Buying (Diff)
    daily_net = signal.diff().fillna(0)
    
    # 2. Smooth it to see the trend of buying/selling
    # Using a 20-day moving average to represent 'monthly' trend
    ma_short = daily_net.rolling(window=10).mean()
    ma_long = daily_net.rolling(window=60).mean()
    
    # 3. Detect Crossovers or Significant Deviations
    # Significant Deviation: When short-term MA deviates from long-term MA significantly
    std_long = daily_net.rolling(window=60).std()
    
    z_score = (ma_short - ma_long) / std_long
    
    # Identify points where Z-score crosses threshold
    # But for "Change Point", we want dates where the Regime changes (e.g., from Selling to Buying)
    
    change_points = []
    
    # Regime: Positive (Buying) vs Negative (Selling)
    # Based on 20-day MA of Daily Net Buying
    regime = np.sign(ma_short)
    regime_changes = regime.diff().fillna(0)
    
    # Sign changes in the trend of buying
    change_indices = regime_changes[regime_changes != 0].index
    
    for idx in change_indices:
        if idx < min_dist: continue
        
        # Check magnitude of the change to filter out noise
        # Compare average net buying 10 days before and 10 days after
        prev_mean = daily_net.iloc[idx-10:idx].mean()
        post_mean = daily_net.iloc[idx:idx+10].mean()
        
        # If the shift is substantial (e.g., > 0.5 standard deviation of the whole series)
        if abs(post_mean - prev_mean) > daily_net.std() * 0.5:
             change_points.append({
                'date': signal.index[idx],
                'type': 'Trend Reversal',
                'desc': f"{'Selling -> Buying' if post_mean > 0 else 'Buying -> Selling'}",
                'score': abs(post_mean - prev_mean)
            })

    # Also detect "Surges" (Intensity shifts without sign change)
    # Using CUSUM-like logic on the derivative
    
    return pd.DataFrame(change_points)

def analyze_skhynix():
    conn = get_db_connection()
    
    # Fetch Data
    query = """
        SELECT dt, samo_fund_net_buy_amount
        FROM tb_stock_investor_invest_accumulation
        WHERE stk_cd = '000660'
        ORDER BY dt ASC
    """
    df = pd.read_sql(query, conn)
    conn.close()
    
    if df.empty:
        print("No data found for SK Hynix.")
        return

    # Preprocessing
    df['dt'] = pd.to_datetime(df['dt'])
    df = df.set_index('dt')
    
    # Filter last 1 year
    latest_date = df.index.max()
    one_year_ago = latest_date - timedelta(days=365)
    df_1y = df[df.index >= one_year_ago].copy()
    
    # Analysis
    # The column is 'Accumulated Amount'.
    # Its slope (derivative) is 'Daily Net Buying Amount'.
    
    series = df_1y['samo_fund_net_buy_amount']
    
    # Apply Detection
    # Using Z-score of Daily Net Buy to find "Shocks"
    daily = series.diff().fillna(0)
    z_scores = (daily - daily.rolling(60).mean()) / daily.rolling(60).std()
    
    print(f"=== [SK하이닉스] 사모펀드 누적 순매수 금액 변곡점 분석 ({one_year_ago.date()} ~ {latest_date.date()}) ===\n")
    
    print("1. 추세 전환점 (Trend Reversal Detect)")
    # Using sliding window mean comparison
    points = []
    window = 20 # 20 days window
    
    for i in range(window, len(daily) - window):
        prev_window = daily.iloc[i-window:i]
        next_window = daily.iloc[i:i+window]
        
        prev_mean = prev_window.mean()
        next_mean = next_window.mean()
        
        # T-test like logic: difference in means relative to variance
        pooled_std = np.sqrt((prev_window.std()**2 + next_window.std()**2) / 2)
        if pooled_std == 0: continue
        
        t_stat = (next_mean - prev_mean) / pooled_std
        
        # If t-stat is high, it means significant shift in buying behavior
        if abs(t_stat) > 1.5: # Threshold
            # Check if it's a local maximum of t-stat to avoid consecutive dates
            # We will filter later
            points.append({
                'date': daily.index[i],
                'stat': t_stat,
                'prev_avg': prev_mean,
                'next_avg': next_mean
            })
            
    df_points = pd.DataFrame(points)
    
    if not df_points.empty:
        # Filter local maxima of 'stat' to get precise dates
        df_points['abs_stat'] = df_points['stat'].abs()
        # Non-maximum suppression logic (simple)
        kept_points = []
        last_date = None
        
        # Sort by date
        df_points = df_points.sort_values('date')
        
        temp_group = []
        for idx, row in df_points.iterrows():
            if last_date is None:
                temp_group.append(row)
                last_date = row['date']
                continue
                
            if (row['date'] - last_date).days <= 10: # Group events within 10 days
                temp_group.append(row)
            else:
                # Find max in group
                best = max(temp_group, key=lambda x: x['abs_stat'])
                kept_points.append(best)
                temp_group = [row]
            
            last_date = row['date']
            
        if temp_group:
            kept_points.append(max(temp_group, key=lambda x: x['abs_stat']))
            
        # Display
        final_df = pd.DataFrame(kept_points)
        final_df = final_df.sort_values('abs_stat', ascending=False)
        
        print(f"\n[분석 결과] 통계적으로 유의미한 수급 변화 시점 (Top 5):\n")
        
        for idx, row in final_df.head(5).iterrows():
            date_str = row['date'].strftime('%Y-%m-%d')
            stat_val = row['stat']
            action = ""
            
            # Interpret
            if row['prev_avg'] < 0 and row['next_avg'] > 0:
                action = "매도 우위 → [강력 매수] 전환"
            elif row['prev_avg'] > 0 and row['next_avg'] < 0:
                action = "매수 우위 → [강력 매도] 전환"
            elif row['prev_avg'] < row['next_avg']:
                action = "매수세 강화 (또는 매도세 약화)"
            else:
                action = "매도세 강화 (또는 매수세 약화)"
                
            amount_diff = (row['next_avg'] - row['prev_avg']) / 100000000 # 억 단위
            
            print(f"- {date_str}: {action}")
            print(f"  (통계적 강도: {stat_val:.2f}, 일평균 순매수 변화: {amount_diff:+.1f}억원)")
            print("")
            
    else:
        print("특이한 변곡점이 발견되지 않았습니다.")

if __name__ == "__main__":
    analyze_skhynix()
