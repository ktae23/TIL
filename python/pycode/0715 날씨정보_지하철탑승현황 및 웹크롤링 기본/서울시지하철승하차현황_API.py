### API를 이용한 지하철 탑승현황 분석 기본

from tqdm import tqdm
from bs4 import BeautifulSoup as bs
import pandas as pd
import requests

### 사용자가 원하는 url 생성 함수
def url_print(tdate, uRow=5):
    uKey = "7672746155736a6436336451676175"
    url_gibon="http://openapi.seoul.go.kr:8088/" + uKey
    url_obj="/xml/CardSubwayStatsNew/"
    url_row="1/"+ str(uRow) +"/"
    url_date=str(tdate)
    url=url_gibon+url_obj+url_row+url_date

    url_soup=html_parser(url)

    return url_soup


### url을 이용한 웹 사이트 파씽
def html_parser(url):
    url_xml = requests.get(url)
    if url_xml.status_code != 200:
        print('데이터를 가져오지 못했습니다.')
        exit()
    soup = bs(url_xml.content, 'html.parser')
    return soup


### 딕셔너리 구조로 저장 후 DataFarame 구조로 변경
def seoul_sw_pandas(seoul_sw_soup):
    xml_row = seoul_sw_soup.find_all('row')
    xml_txt = []
    for row in xml_row:
        dt = row.find('use_dt').text               # 사용일자
        line = row.find('line_num').text           # 호선(라인)
        sub_sta = row.find('sub_sta_nm').text      # 역이름
        ride = row.find('ride_pasgr_num').text     # 승차총인원
        alight = row.find('alight_pasgr_num').text # 하차총인원

        xml_txt.append({'사용일': dt, '라인': line, '역명': sub_sta, '승차': ride, '하차': alight})

    #print(xml_txt)
    df = pd.DataFrame(xml_txt)
    return df


# 시작일부터 종료일까지의 날짜문자 리스트를 이용해 전체 일자별 데이터 조회 pandas.concat()을 이용한 행추가
def main_api(sDt, eDt):
    # pd.date_range(start=staDate, end=endDate) : 시작일부터 종료일까지 날짜 생성, 시작일/종료일=>문자형
    dt_index = pd.date_range(start=str(sDt), end=str(eDt))
    dtList = dt_index.strftime("%Y%m%d").tolist()  # 날짜형을 문자형으로 변경후 리스트형으로 저장

    df0 = pd.DataFrame()  # 전체 데이터 저장 변수

    for sDt in tqdm(dtList, desc="진행율: "):
        seoul_sw_soup = url_print(sDt)  # 한페이지에 5개의 데이터가 출력된 url 정보 가져오기
        #if seoul_sw_soup.find('code').text == "INFO-200":
        #    continue
        try:
            uRow = seoul_sw_soup.find('list_total_count').text  # 조회된 전체 데이터 개수 추출하기
            seoul_sw_soup = url_print(sDt, uRow)  # 한페이지에 추출한 전체 데이터 출력 url 정보 가져오기
            df = seoul_sw_pandas(seoul_sw_soup)  # 요청 데이터에 대한 DataFrame 형식으로 변경하기
            df0 = pd.concat([df0, df], ignore_index=True)  # ignore_index=True: 인덱스 재배열(재설정)
        except:
            continue
    print('작업완료')
    return df0


if __name__ == "__main__":
    sDt = input('시작일 입력(예: 20210101): ')
    eDt = input('종료일 입력(예: 20210101): ')

    df0 = main_api(sDt, eDt)
    df0.to_csv('c:/pydata/seoul_sw_inout.csv', index=False)
    # utf-8 형식으로 index 없이 저장
    print('===== 작업종료 =====')






