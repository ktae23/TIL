import pandas as pd
import matplotlib.pyplot as plt


df1=pd.read_csv('./data/서울시 인구현황_구.txt', sep='\t', header=1)
df1=df1.iloc[:, [0,1,3,4,5,6,7,8,9,10,11,-1]]
df1.drop(0, inplace=True)

col_name=['년도', '자치구', '총인구', '총인구(남)', '총인구(여)', '내국인', '내국인(남)', '내국인(여)',
          '외국인', '외국인(남)','외국인(여)', '65세이상']

for i in range(len(col_name)):
    df1.rename(columns={df1.columns[i]:col_name[i]}, inplace=True)

df2=df1.iloc[:, [0,1,2,5,8,11]]

for row_i in df2.index:
    df2.loc[row_i, "총인구"]=df2.loc[row_i, "총인구"].replace(",","")
    df2.loc[row_i, "내국인"]=df2.loc[row_i, "내국인"].replace(",", "")
    df2.loc[row_i, "외국인"]=df2.loc[row_i, "외국인"].replace(",", "")
    df2.loc[row_i, "65세이상"]=df2.loc[row_i, "65세이상"].replace(",", "")

df2=df2[df2['내국인'] != "…"]  # 1991년 데이터 제거

df2=df2.astype({'총인구':int, '내국인':int,'외국인':int,'65세이상':int})
print(df2.dtypes)


import matplotlib.font_manager as fm
font_name=fm.FontProperties(fname="C:/Windows/Fonts/malgun.ttf").get_name()
plt.rc('font', family=font_name)

gu=input('조회 구이름 입력: ')
df3=df2[df2['자치구'] == gu]

plt.figure(figsize=(10, 4))
plt.style.use('ggplot')
plt.xticks(size=10, rotation=45)

plt.plot(df3['년도'], df3['내국인'], marker='o', markersize=5, label='내국인')
plt.plot(df3['년도'], df3['외국인'], marker='o', markersize=5, label='외국인')
plt.plot(df3['년도'], df3['65세이상'], marker='o', markersize=5, label='65세이상')

plt.legend()
plt.title('서울시 내/외국인 및 고령자 현황 분석 그래프(1992~2020 기준)', size=15)
plt.xlabel('년도')
plt.ylabel('인구수')

plt.show()

### [년도별 인구현환 분석]
### 1. 사용자가 입력된 년도의 데이터를 이용해 외국인 현황
### 2. x축으로 자치구 사용
### 3. 막대그래프, 제목, x/y 축제목

### 4. 외국인 남성/여성의 구성 비율(자치구별 확인)

