import requests
from bs4 import BeautifulSoup as bs
import pandas as pd
import matplotlib.pyplot as plt
import datetime
from tqdm import tqdm

from matplotlib import font_manager
font_family = font_manager.FontProperties(fname='C:/Windows/Fonts/malgunsl.ttf').get_name()
plt.rc('font', family=font_family)


### url을 이용해 데이터 받아오기
def url_parser(sDate, eDate, row=10):
    key = "개인키"

    url = "http://openapi.data.go.kr/openapi/service/rest/Covid19/getCovid19SidoInfStateJson?serviceKey=" + key
    url = url + "&pageNo=1&numOfRows=" + str(row)
    url = url + "&startCreateDt=" + str(sDate)
    url = url + "&endCreateDt=" + str(eDate)

    xml_soup = requests.get(url)

    if xml_soup.status_code != 200:
        print('API 연결 오류')
        exit()

    soup = bs(xml_soup.text, 'html.parser')

    return soup

### 코로나 데이터 DataFrame 형식으로 변경
def covid_info(soup):
    items = soup.find_all('item')

    covid_list = []
    for item in tqdm(items, desc='진행율='):
        stdDay = item.find('stdday').get_text()  # 기준일
        stdDay = stdDay.replace("년 ", "").replace("월 ", "")[0:8]
        ### 년,월 글자 제거 후 8자리 가져오기(일 00시, 일 18시, 일 17시 등 제거)
        gubun = item.find('gubun').get_text()  # 지역명
        incDec = item.find('incdec').get_text()  # 당일 확진자
        defCnt = item.find('defcnt').get_text()  # 누적 확진자
        localCnt = item.find('localocccnt').get_text()  # 지역
        overCnt = item.find('overflowcnt').get_text()  # 해외

        covid_list.append([stdDay, gubun, incDec, localCnt, overCnt, defCnt])

    df1 = pd.DataFrame(covid_list, columns=['기준일', '지역명', '당일', '지역', '해외', '누적'])
    df1 = df1.astype({'당일': "int64", "지역": "int64", "해외": "int64", "누적": "int64"})  # 데이터 형식 변경
    df1.sort_values(by='기준일', inplace=True)  # 기준일을 기준으로 오름차순 정렬

    return df1


### 문자형 데이터를 날짜형으로 변경

import datetime

print(datetime.datetime.strptime("2021년 07월 14일 00시", "%Y년 %m월 %d일 00시").strftime("%Y%m%d"))
print("2021년 07월 14일 00시".replace("년 ","").replace("월 ","").replace("일 00시",""))


sDate='20200701'
eDate=datetime.datetime.today().strftime('%Y%m%d')

soup=url_parser(sDate, eDate)
row=soup.find('totalcount').text
soup=url_parser(sDate, eDate, row)
df1=covid_info(soup)

df1['기준일']=pd.to_datetime(df1['기준일'])

# 지역별 조회 후 차트 표시
df2=df1[df1['지역명']=='대구']
df2.info()

plt.style.use('ggplot')
plt.figure(figsize=(14, 5))

plt.plot(df2.기준일, df2['당일'], marker="o", markersize=3)
plt.show()


### 원하는 지역 죄회 후 전체 데이터와 같이 출력하기
scLoc="서울"  #input('조회 지역 입력:')

df2=df1[df1['지역명']==scLoc]
df3=df1[df1['지역명']=='합계']

plt.style.use('ggplot')
plt.figure(figsize=(14, 5))

plt.subplot(2,1,1)
plt.plot(df2.기준일, df2['당일'], marker="o", markersize=3, label=scLoc)
plt.subplot(2,1,2)
plt.plot(df3.기준일, df3['당일'], marker="o", markersize=3, label='전체')
plt.legend()

plt.show()


### 현재 등록된 지역 전체를 차트로 출력하기
loc_list = list(df1.지역명.unique())

for locName in loc_list:
    plt.style.use('ggplot')
    plt.figure(figsize=(14, 5))

    df2 = df1[df1['지역명'] == locName]
    plt.plot(df2.기준일, df2['당일'], marker="o", markersize=3)

    if locName == '합계':
        plt.title('전체 코로나 발생 현황')
    else:
        plt.title(locName + ' 코로나 발생 현황')

    plt.show()




