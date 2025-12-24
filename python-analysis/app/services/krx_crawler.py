import FinanceDataReader as fdr
import pandas as pd
from models.stock_dto import StockDto

class KrxCrawler:
    def get_kospi_list(self):
        print("KRX에서 KOSPI 데이터를 가져오는 중입니다...")
        
        # 1. FinanceDataReader를 이용해 KOSPI 전체 리스트 가져오기
        # (KOSPI, KOSDAQ 등 지정 가능)
        df = fdr.StockListing('KOSPI')
        
        # 2. 데이터프레임(DataFrame)을 우리가 만든 DTO 리스트로 변환
        stock_list = []
        
        # iterrows()는 데이터를 한 줄씩 읽습니다.
        for index, row in df.iterrows():
            # 데이터가 없는 경우(NaN)를 위해 안전하게 처리
            sector = row['Sector'] if 'Sector' in df.columns and pd.notna(row['Sector']) else ''
            industry = row['Industry'] if 'Industry' in df.columns and pd.notna(row['Industry']) else ''
            
            dto = StockDto(
                code=row['Code'],
                name=row['Name'],
                market='KOSPI',
                sector=sector,
                industry=industry
            )
            stock_list.append(dto)
            
        print(f"총 {len(stock_list)}개의 종목을 가져왔습니다.")
        return stock_list