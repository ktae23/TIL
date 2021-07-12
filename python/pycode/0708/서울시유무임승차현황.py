from tqdm import tqdm
from bs4 import BeautifulSoup as bs
import pandas as pd
import requests

### 사용자가 원하는 url 생성 함수
def url_parser(tmonth, uRow=5):
    uKey = "7672746155736a6436336451676175"
    url="http://openapi.seoul.go.kr:8088/" + uKey
    url=url+"/xml/CardSubwayPayFree/1/"+ str(uRow) +"/"
    url=url+str(tmonth)

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
        dm = row.find('use_mon').text               # 사용일자
        line = row.find('line_num').text            # 호선(라인)
        sub_sta = row.find('sub_sta_nm').text       # 역이름
        pay_ride = row.find('pay_ride_num').text  # 유임승차총인원
        free_ride = row.find('free_ride_num').text  # 무임승차총인원
        pay_alight = row.find('pay_alight_num').text  # 유임하차총인원
        free_alight = row.find('free_alight_num').text  # 무임하차총인원

        xml_txt.append({'사용일': dm, '라인': line, '역명': sub_sta, '유임승차': pay_ride, '무임승차':free_ride,
                        '유임하차':pay_alight ,'무임하차':free_alight})

    #print(xml_txt)
    df = pd.DataFrame(xml_txt)
    return df

# 시작일부터 종료일까지의 날짜문자 리스트를 이용해 전체 일자별 데이터 조회 pandas.concat()을 이용한 행추가
def main_api(sDt, eDt):
    # pd.date_range(start=staDate, end=endDate) : 시작일부터 종료일까지 날짜 생성, 시작일/종료일=>문자형
    sDt=sDt+'01'
    eDt=eDt+"01"
    dt_index = pd.date_range(start=sDt, end=eDt,  freq='MS')
    dtList = dt_index.strftime("%Y%m").tolist()  # 날짜형을 문자형으로 변경후 리스트형으로 저장
    df0 = pd.DataFrame()  # 전체 데이터 저장 변수

    for dtl in tqdm(dtList, desc="진행율: "):
        seoul_sw_soup=url_parser(dtl)  # 한페이지에 5개의 데이터가 출력된 url 정보 가져오기
        if seoul_sw_soup.find('code').text == "INFO-200":
            continue
        uRow = seoul_sw_soup.find('list_total_count').text  # 조회된 전체 데이터 개수 추출하기
        seoul_sw_soup=url_parser(dtl, uRow)  # 한페이지에 추출한 전체 데이터 출력 url 정보 가져오기
        df = seoul_sw_pandas(seoul_sw_soup)  # 요청 데이터에 대한 DataFrame 형식으로 변경하기

        df0 = pd.concat([df0, df], ignore_index=True)  # ignore_index=True: 인덱스 재배열(재설정)

    return df0


if __name__ == "__main__":
    sDt = input('시작년월 입력(예: 201501): ')
    eDt = input('종료년월 입력(예: 202001): ')

    df0 = main_api(sDt, eDt)
    df0.to_csv('c:/pydata/seoul_sw_payfree.csv', index=False)
    print('===== 작업종료 =====')

    print(df0)

