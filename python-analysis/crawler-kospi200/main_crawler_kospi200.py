from kospi200_crawler import Kospi200Crawler
import pandas as pd
import os


def main():
    # 1. 크롤러 서비스 생성
    crawler = Kospi200Crawler()
    
    # 2. KOSPI200 리스트 가져오기
    stocks = crawler.get_kospi200_list()
    
    # 3. 결과 출력 (상위 10개만)
    print("\n[상위 10개 종목 미리보기]")
    for stock in stocks[:10]:
        print(stock)
    
    # 4. CSV 파일로 저장
    df_result = pd.DataFrame([vars(s) for s in stocks])
    
    save_path = "kospi200_list.csv"
    df_result.to_csv(save_path, index=False, encoding='utf-8-sig')
    print(f"\n파일이 저장되었습니다: {os.path.abspath(save_path)}")


if __name__ == "__main__":
    main()
