#!/usr/bin/env python
# coding: utf-8

# In[36]:


import requests
from bs4 import BeautifulSoup as bs
import pandas as pd
from tqdm import tqdm


# ### API 연결 및 데이터 파씽

# In[37]:


def url_parser(sDate, eDate, row=10):
    ### API 연결 및 데이터 파씽
    key="VFYXI%2F3A5kIk7QVsO1epAlZSb6ZtCO9N5VC%2FMF3pW6oZ7%2BsFrvH1Y9y0UfmVzTnA0k2FasoNM6t6JsY7%2BEwNEw%3D%3D"

    # 기본 url + 사용자 key
    url="http://api.visitkorea.or.kr/openapi/service/rest/KorService/searchFestival?serviceKey=" + key
    url=url+"&MobileOS=ETC&MobileApp=AppTest&arrange=A&listYN=Y"   # 기본 필수 옵션
    url=url+"&numOfRows="+ str(row)   # 한페이지 출력 데이터 개수
    url=url+"&eventStartDate="+str(sDate)  # 조회 시작일
    url=url+"&eventEndDate="+str(eDate)
    #print(url+"\n\n")
    xml_soup=requests.get(url)
    if xml_soup.status_code != 200:
        print("API를 연결하지 못하였습니다.")
        exit()
        
    soup=bs(xml_soup.content, 'html.parser')  # xml_soup.text 변경 가능
    
    return soup


# ### 파씽한 데이터에서 원하는 데이터 추출 및 DataFrame로 변경

# In[38]:


def tour_info(sDate, eDate, soup):
    ### 파씽한 데이터에서 원하는 데이터 추출 및 DataFrame로 변경
    row=soup.find('totalcount').text
    re_soup=url_parser(sDate, eDate, row)
    
    items=re_soup.find_all('item')
    df_list=[]
    
    for item in tqdm(items, desc="진행:"):
        staD=item.find('eventstartdate').text      # 축제 시작일
        endD=item.find('eventenddate').get_text()  # 축제 종료일
        titD=item.find('title').get_text()         # 축제명
        try:
            addD=item.find('addr1').get_text()         # 주소
        except:
            addD="온라인개최"
        try:
            telD=item.find('tel').get_text()           # 전화
        except:
            telD="-"
        try:
            lngD=item.find('mapx').text                # 경도
            latD=item.find('mapy').get_text()          # 위도
        except:
            lngD=0
            latD=0
        
        df_list.append({"시작일":staD, "종료일":endD, "축제명":titD,
                        "주소":addD, "연락처":telD, '위도':latD, '경도':lngD})
        
    df1=pd.DataFrame(df_list)
    
    return df1


# ### 코드 실행

# In[45]:


# if __name__=="__main__":
sDate=input("조회 시작일:(예:20200101):")
eDate=input("조회 종료일:(예:20201231):")

soup=url_parser(sDate, eDate)
df1=tour_info(sDate, eDate, soup)
df1.to_csv('c:/pydata/tour_'+str(sDate)+"_"+str(eDate)+".csv", index=False)

print(df1)


# ### 축제 데이터 분석
# - 1. 2020년~2021년 축제 건수 비교(시작일 기준)
# - 2. 축제유형(집합/비대면 축제)
# - 3. 지역별 축제 현황

# In[46]:


cnt19=0;cnt20=0;cnt21=0

for cntDate in df1['시작일']:
    if cntDate[:4] == "2019":
        cnt19 += 1
    elif cntDate[:4] == "2020":
        cnt20 += 1
    else:
        cnt21 += 1
        
print(cnt19, cnt20, cnt21)
        


# In[47]:


df1['시작년']=df1['시작일'].str[:4]
df2=df1.groupby('시작년')['축제명'].count()


# In[48]:


import matplotlib.pyplot as plt

df2.plot.bar()
plt.show()


# In[49]:


df1['시작일']=pd.to_datetime(df1['시작일'])
df1['종료일']=pd.to_datetime(df1.종료일)
df1.dtypes


# In[64]:


df1['시작년']=df1['시작일'].dt.year
df1['시작월']=df1['시작일'].dt.month

df2=df1.groupby('시작년')['축제명'].count()
df2.plot.barh()


# - 축제 시작년/시작월을 기분으로 groupby 
# - concat()으로 열 추가하기

# In[154]:


df2=df1.groupby(['시작년', '시작월'])['축제명'].count()   # 축제 시작년/시작월 기준 건수 확인
df2=df2.reset_index()   # '시작년', '시작월'을 데이터로 사용하기 위해 index 재설정
#print(df2[df2['시작년']==2021].set_index('시작월'))

df3 = df2[df2['시작년']==2021].set_index('시작월')   # index 시작월로 재설정
df4 = df2[df2['시작년']==2020].set_index('시작월')

df3 = df3.rename(columns = {df3.columns[1]: '2021Y'})       # 각각의 재설정 데이터의 열 이름 변경
df4 = df4.rename(columns = {df4.columns[1]: '2020Y'})

df2 = pd.concat([df3, df4], axis = 1)    # df3를 기준으로 열 추가 => 결과 df2 저장
del df3, df4  # df3, df4 삭제

df2.drop('시작년', axis=1, inplace=True)
print(df2)


# In[155]:


df3=df1.groupby(['시작년', '시작월'])['축제명'].count()
df3=df3.reset_index()
#print(df2[df2['시작년']==2021].set_index('시작월'))

df3=pd.concat([df3[df3['시작년']==2021].set_index('시작월'),
               df3[df3['시작년']==2020].set_index('시작월')], axis=1)
df3.drop('시작년', axis=1, inplace=True)
print(df3)


# In[158]:


df2=df2.fillna(0)
df3=df3.fillna(0)
print(df2["2020Y"])
print(df3.iloc[:, 1])


# In[159]:


df2.plot.bar()
plt.show()


# In[166]:


df3.iloc[:, 0].plot.bar()
plt.title('2021Y')
plt.show()
df3.iloc[:, 1].plot.bar()
plt.title('2020Y')
plt.show()


# In[170]:


print(df1[(df1['주소']=="온라인개최")&(df1['시작년']==2020)]['주소'].count())
print(df1[(df1['주소']!="온라인개최")&(df1['시작년']==2020)]['주소'].count())


# In[171]:


print(df1[(df1['주소']=="온라인개최")&(df1['시작년']==2021)]['주소'].count())
print(df1[(df1['주소']!="온라인개최")&(df1['시작년']==2021)]['주소'].count())


# In[172]:


pList=[]
pList.append([df1[(df1['주소']=="온라인개최")&(df1['시작년']==2020)]['주소'].count(),
              df1[(df1['주소']!="온라인개최")&(df1['시작년']==2020)]['주소'].count()])
pList.append([df1[(df1['주소']=="온라인개최")&(df1['시작년']==2021)]['주소'].count(),
              df1[(df1['주소']!="온라인개최")&(df1['시작년']==2021)]['주소'].count()])

pList


# In[201]:


df2=pd.DataFrame(pList, columns=["온라인", '오프라인'], index=["2020년", "2021년"])
print(df2)


# In[193]:


from matplotlib import font_manager

font_family = font_manager.FontProperties(fname='C:/Windows/Fonts/malgunsl.ttf').get_name()
plt.rc('font', family=font_family)

df2.plot.bar(rot=0)

plt.show()


# In[196]:


#!pip install seaborn


# In[215]:


import seaborn as sns
fig=plt.figure(figsize=(10, 5))

#ax1=fig.add_subplot(1, 2, 1)
#ax2=fig.add_subplot(1, 2, 2)

plt.subplot(1,2,1)
x=list(df2.iloc[0])
xlab=df2.columns
plt.pie(x, labels=xlab, autopct="%.1f%%")
plt.title('2020년')

plt.subplot(1,2,2)
x=list(df2.iloc[1])
xlab=df2.columns
plt.pie(x, labels=xlab, autopct="%.1f%%")
plt.title('2021년')

plt.show()


# In[207]:


[df2.iloc[0, 0], df2.iloc[0, 1]]


# In[210]:


print(list(df2.loc["2020년"]))
print(list(df2.iloc[0]))


# #### 지역별 축제 현황[각자 코딩]
# - 주소를 " " 기준으로 나누어 첫번째 값 선택
# - 해당 값을 이용해서 '지역' 열 생성 및 데이터 추가
# - 지역별 축제 현황 분석(현황 테이블화, 시각화-막대그래프, 원그래프, *히트맵 그래프)

# In[ ]:





# ### 지도에 축제 위치 출력하기
# - folium 모듈

# In[218]:


#!pip install folium


# In[219]:


import folium


# In[222]:


umap=folium.Map(location=[37.503386, 127.049797], zoom_start=15)    # 기본 맵 생성, zoom_start=15 :확대/축소

### 생성된 맵에 Marker(위치기호)를 추가한다.
folium.Marker(location=[37.503386, 127.049797], 
              icon=folium.Icon(color='red'), 
              popup='강의장').add_to(umap)

umap.save('c:/pydata/강의장.html')


# In[226]:


df2=df1[(df1['위도']!=0)&(df1['시작년']==2021)]

umap=folium.Map(location=[37.503386, 127.049797], zoom_start=15)

for inx in df2.index:
    folium.Marker(location=[df2.loc[inx,"위도"], df2.loc[inx,'경도']], 
                  icon=folium.Icon(color='red'), 
                  popup=df2.loc[inx,"축제명"]).add_to(umap)

umap.save('c:/pydata/2021년_축제정보.html')


# In[235]:


umap=folium.Map(location=[37.503386, 127.049797], zoom_start=15) 

### 지도에 동그라미 표시
folium.CircleMarker([37.503386, 127.049797],
                    color="red",
                    fill_color="#ffffgg",
                    radius = 10, 
                    popup="강의장").add_to(umap)

umap


# In[ ]:





# In[ ]:




