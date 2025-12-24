from pykrx import stock
from datetime import datetime
import pandas as pd
from stock_dto import StockDto


class Kospi200Crawler:
    def get_kospi200_list(self):
        """KOSPI200 종목 리스트를 가져옵니다."""
        print("KOSPI200 종목 데이터를 가져오는 중입니다...")
        
        # KOSPI200 종목 코드 가져오기 (1028 = KOSPI200 인덱스 코드)
        tickers = stock.get_index_portfolio_deposit_file("1028")
        
        stock_list = []
        
        # 각 종목의 상세 정보 가져오기
        for ticker in tickers:
            try:
                # 종목명 가져오기
                name = stock.get_market_ticker_name(ticker)
                
                dto = StockDto(
                    code=ticker,
                    name=name,
                    market='KOSPI200',
                    sector='',
                    industry=''
                )
                stock_list.append(dto)
                
            except Exception as e:
                print(f"종목 {ticker} 처리 중 오류 발생: {e}")
                continue
        
        print(f"총 {len(stock_list)}개의 KOSPI200 종목을 가져왔습니다.")
        return stock_list
