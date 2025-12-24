from services.krx_crawler import KrxCrawler
import pandas as pd
import os

def main():
    # 1. 크롤러 서비스 생성
    crawler = KrxCrawler()
    
    # 2. KOSPI 리스트 가져오기
    stocks = crawler.get_kospi_list()
    
    # 3. 결과 출력 (상위 5개만)
    print("\n[상위 5개 종목 미리보기]")
    for stock in stocks[:5]:
        print(stock)

    # 4. CSV 파일로 저장 (결과 확인용)
    # DTO 리스트를 다시 보기 편하게 DataFrame으로 변환
    df_result = pd.DataFrame([vars(s) for s in stocks])
    
    save_path = "kospi_list.csv"
    df_result.to_csv(save_path, index=False, encoding='utf-8-sig')
    print(f"\n파일이 저장되었습니다: {os.path.abspath(save_path)}")

if __name__ == "__main__":
    main()