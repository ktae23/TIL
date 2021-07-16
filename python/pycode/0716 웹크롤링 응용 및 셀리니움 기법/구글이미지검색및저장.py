from selenium import webdriver
from bs4 import BeautifulSoup as bs
import urllib.request
import os
from tqdm import tqdm
import time

keyword = input('검색 이미지 검색어를 입력하세요.')
url = "https://www.google.com/search?q=" + keyword
url = url + "&rlz=1C1CHBD_koKR943KR943&sxsrf=ALeKk037tHKhmpuSVS0MH-gEqtMXMU64dg:1626412085656&source=lnms&tbm=isch&sa=X&ved=2ahUKEwizw_6U6ebxAhUVyYsBHfYHBtwQ_AUoAXoECAEQAw&biw=2660&bih=1515&dpr=1.2"

driver = webdriver.Chrome('C:/pydata/chromedriver.exe')
driver.get(url)
time.sleep(2)

### 이미지를 화면에 최대한 많이 나타나게 함(스크롤 내리고 더보기 버튼 클릭)
for i in range(10):
    if i == 5:
        # 더보기 버튼 클릭
        driver.find_element_by_css_selector(
            "#islmp > div > div > div > div.gBPM8 > div.qvfT1 > div.YstHxe > input").click()
        time.sleep(2)
        
    # 스크롤 내리기
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(2)


### 이미지 src 속성값 가져오기
html_source=driver.page_source
soup=bs(html_source, "html.parser")

img_soup=soup.select("img.rg_i.Q4LuWd")

img_list=[]

for img in tqdm(img_soup, desc="링크="):
    try:
        img_list.append(img['src'])
    except:
        img_list.append(img['data-src'])

##### 중복 데이터 제거
print("중복 제거전 사진수:", len(img_list))
img_list=set(img_list)   # 중복 제거
print("중복 제거후 사진수:", len(img_list))


### 저장할 폴더 생성
fDir = "c:/pydata/image/"
fName = os.listdir(fDir)

### 저장할 폴더 존재여부 확인
fName_dir = keyword + "0"
cnt = 0

while True:
    if fName_dir not in fName:  # 새로 생성한 폴더가 현재 저장 위치에 있다/없다 결정
        os.makedirs(fDir + fName_dir)  # 없으면 현재 이름으로 폴더 생성
        break  # 폴더 생성 후 while 문 빠져나가기
    cnt += 1
    fName_dir = keyword + str(cnt)  # 새로운 폴더명 생성

print(fName_dir, "로 폴더 생성")


### 검색 이미지 저장
cnt = 0
for img in tqdm(img_list):
    sfdir = fDir + fName_dir + "/" + keyword + str(cnt) + '.jpg'  # 저장 경로와 파일명 설정
    urllib.request.urlretrieve(img, sfdir)  # 이미지 저장하기
    cnt += 1

driver.close()
print('검색 이미지 저장 완료')