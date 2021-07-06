#!/usr/bin/env python
# coding: utf-8

# ### [목표]
# - 년도별 서울시 인구 현황(내국인/외국인/고령자)
# - 년도별 외국인 남/여 비율

# In[25]:


# 구분자가 콤마(,)가 아닌 다른 구분자(탭키:\t)를 사용한 데이터 읽어오기
# 제목행 선택: header=1
import pandas as pd

df1=pd.read_csv('./data/서울시 인구현황_구.txt', sep="\t", header=1)
df1.head()


# In[26]:


# 기간/자치구/전체인구/전체(남)/전체(여)/내국인/내국인(남)/내국인(여)/외국인/외국인(남)/외국인(여)/65세이상
df1[['기간', '자치구','합계', '합계.1', '합계.2', '한국인', '한국인.1', '한국인.2',
     '등록외국인', '등록외국인.1', '등록외국인.2','65세이상고령자']]

df1.loc[:, ['기간', '자치구','합계', '합계.1', '합계.2', '한국인', '한국인.1', '한국인.2',
     '등록외국인', '등록외국인.1', '등록외국인.2','65세이상고령자']]

df1=df1.iloc[:, [0,1,3,4,5,6,7,8,9,10,11,-1]]
df1.head()


# #### ※ 참고사항 : axis = 0 vs axis = 1
# -. axis = 0은 dataframe 행 단위를 수정할 때 필요한 파라미터 값이다.
# -. axis = 1은 dataframe 열 단위를 수정할 때 필요한 파라미터 값이다.
# -. ![image.png](attachment:image.png)
# -. drop( ) 함수에 index, column이라는 파라미터를 사용하지 않는다면 axis=0 또는 axis=1 파라미터값을 넣어줘야 한다.

# In[27]:


df1.drop(0, inplace=True) # axis=0 생략


# In[28]:


df1.head()


# ### 위 내용 합친 코드

# In[69]:


# 구분자가 콤마(,)가 아닌 다른 구분자(탭키:\t)를 사용한 데이터 읽어오기
# 제목행 선택: header=1
import pandas as pd

df1=pd.read_csv('./data/서울시 인구현황_구.txt', sep="\t", header=1)
df1=df1.iloc[:, [0,1,3,4,5,6,7,8,9,10,11,-1]]
df1.drop(0, inplace=True) # axis=0 생략
print(df1.head(3))


# ### 열 이름 변경
# - df.rename(columns={'합계':'총인구', '합계.1':'총인구(남), ...})  # 전체 열이름 일부 변경
# - df.rename(columns={df1.columns[2]:'총인구', df1.columns[2]:'총인구(남), ...}) # 열 이름을 많이 변경

# In[70]:


df1=df1.rename(columns={'합계':'총인구', '한국인':'내국인', '등록외국인':'외국인'})
print(df1.head(3))


# In[71]:


# df1.rename(columns={df1.columns[0]:"년도", 
#                     df1.columns[1]:"자치구",
#                     df1.columns[2]:"총인구",
#                     df1.columns[3]:"총인구(남)",
#                     df1.columns[4]:"총인구(여)", ......
#                     })


# In[72]:


col_name=['년도', '자치구', '총인구', '총인구(남)', '총인구(여)', '내국인', '내국인(남)', '내국인(여)',
          '외국인', '외국인(남)','외국인(여)', '65세이상']

for i in range(len(col_name)):
    df1.rename(columns={df1.columns[i]:col_name[i]}, inplace=True)

print(df1.head(3))


# ### 년도별 총인구(남)/총인구(여) 값을 나타내는 그래프 작성

# In[73]:


print(df1.info())


# ### df1에서 년도/자치구/총인구/총인구(남)/총인구(여) 값만 추출

# In[74]:


df2=df1.iloc[:, 0:5]
print(df2.head())
print(df2.info())


# ### DataFrame 데이터 타입 변경하는 방법
# - ![image.png](attachment:image.png)

# In[75]:


# pandas에서 데이터형을 원하는 데이터형으로 변경: astype() 함수

df2['총인구']=df2["총인구"].str.replace(',', "")
df2['총인구(남)']=df2['총인구(남)'].str.replace(',', "")
df2['총인구(여)']=df2['총인구(여)'].str.replace(',', "")
print(df2.dtypes)


# In[76]:


df2=df2.astype({'년도':int,'총인구':int,'총인구(남)':int,'총인구(여)':'int64'})
print(df2.dtypes)


# In[77]:


print(df2.head())


# In[81]:


import matplotlib.pyplot as plt

gu=input('조회할 구 이름 입력:')

df3=df2[df2['자치구']==gu]
plt.plot(df3['년도'], df3['총인구'])
plt.show()

plt.plot(df3['년도'], df3['총인구(남)'])
plt.plot(df3['년도'], df3['총인구(여)'])

plt.show()


# In[87]:


import matplotlib.font_manager as fm
font_name=fm.FontProperties(fname="C:/Windows/Fonts/malgun.ttf").get_name()
plt.rc('font', family=font_name)

df3.plot(kind='hexbin', x='총인구', y='년도', gridsize=20)  # 산점도 그래프
plt.show()


# In[88]:


df3.plot(kind='bar', x='년도', y=['총인구(남)', '총인구(여)'])  
plt.show()


# In[89]:


df1[df1['내국인'] != '…']


# In[ ]:




