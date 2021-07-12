import pandas as pd
import matplotlib.pyplot as plt
import os
import matplotlib.font_manager as fm
import csv

### 한글 표기
font_name = fm.FontProperties(fname="C:/Windows/Fonts/malgun.ttf").get_name()
plt.rc('font', family=font_name)


def Csv_reset(fn):
    ### csv 파일 전처리 함수
    if fn == "CARD_SUBWAY_MONTH_202009.csv":
        f = open('c:/pydata/subway/' + fn, encoding='cp949')
    else:
        f = open('c:/pydata/subway/' + fn, encoding='utf-8')

    data = csv.reader(f)
    # "사용일자"', '노선명', '역명', '승차총승객수', '하차총승객수', '등록일자'

    next(data)
    data_lst = []
    for row in data:
        data_lst.append(row[:6])

    df = pd.DataFrame(data_lst, columns=['사용일자', '노선명', '역명', '승차총승객수', '하차총승객수', '등록일자'])
    df.to_csv('c:/pydata/subway/' + fn, encoding='cp949', index=False)
    f.close()


def file_read():
    ### subway 폴더에 있는 모든 파일 하나로 만들기
    filePath = 'c:/pydata/subway/'
    fileName = os.listdir(filePath)  # subway 폴더의 모든 파일 및 하위 폴더 정보를 리스트형으로 가져와 저장

    df1 = pd.DataFrame()

    for fn in fileName:
        ### try ~ except : 예외처리 구문
        # print(fn)
        if fn == "CARD_SUBWAY_MONTH_202009.csv": Csv_reset(fn)

        try:  # 정상코드 수행
            df2 = pd.read_csv(filePath + fn, encoding='cp949')
        except:  # try 구문에서 에러 발생시 처리하기 위한 구문
            Csv_reset(fn)
            df2 = pd.read_csv(filePath + fn, encoding='cp949')
        # 데이터 한개의 파일로 만들기
        df1 = pd.concat([df1, df2])

    ## 데이터 전처리
    df1 = df1.reset_index(drop=True)
    ## 데이터 타입 변경
    df1 = df1.astype({'사용일자': str, "승차총승객수": "int64", "하차총승객수": "int64"})

    ## 등록일자 삭제: df.drop( [ '삭제할 열 이름 1', '삭제할 열 이름 2',... ], axis = 1)
    df1.drop(['등록일자'], axis=1, inplace=True)

    ## pandas를 이용한 날짜 변경: pandas.to_datetime(df['필드명'])
    # print(pd.to_datetime(df1['사용일자']))

    df1['년'] = pd.to_datetime(df1['사용일자']).dt.year
    df1['월'] = pd.to_datetime(df1['사용일자']).dt.month
    return df1


def subway_sch1(dfdata, subway_name):
    df2 = dfdata[dfdata['역명'] == subway_name]
    # print(df2.info())
    df2 = df2.astype({'승차총승객수': 'int64', '사용일자': 'str'})
    df2.plot(x='사용일자', y='승차총승객수')
    plt.show()


def subway_sch2(dfdata, subway_name):
    df2 = dfdata[dfdata['역명'].str.contains(subway_name)]
    # print(df2.info())
    df2 = df2.astype({'승차총승객수': 'int64', '사용일자': 'str'})
    df2 = df2.groupby('사용일자')[['승차총승객수', '하차총승객수']].sum()
    df2.plot()
    plt.show()


def subway_sch3(dfdata, subway_name):
    #iYear = int(input('조회 년도:'))
    df3 = dfdata[(dfdata['역명'].str.contains(subway_name))& (dfdata['년'] == 2019)]
    df3 = df3.astype({'승차총승객수': 'int64', '사용일자': 'str'})
    df3 = df3.groupby('월')['승차총승객수'].sum()
    df4 = dfdata[(dfdata['역명'].str.contains(subway_name)) & (dfdata['년'] == 2020)]
    df4 = df4.astype({'승차총승객수': 'int64', '사용일자': 'str'})
    df4 = df4.groupby('월')['승차총승객수'].sum()
    df3.plot()
    df4.plot()
    plt.show()
    del df3, df4

### 사용자 함수 호출 부분
df1 = file_read()
#print(df1.head())

cho=input('조회 방법 선택:\n1.일자별 / 2.월별\n')
subway_name=input('조회역 입력:')

if cho == "1":
    subway_sch2(df1, subway_name)
elif cho == "2":
    subway_sch3(df1, subway_name)
else:
    print('1 또는 2만 입력하세요.')
    exit()




