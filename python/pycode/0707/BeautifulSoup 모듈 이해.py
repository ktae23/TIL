import googlemaps   # Json 형식으로 제공(딕셔너리 구조)
from bs4 import BeautifulSoup as bs

def gglmaps():
    user_Key='AIzaSyCSSzVO1zIV8zfJUdq6b7l2tUQqLk6cbSA'
    gmaps=googlemaps.Client(key=user_Key)

    sch=input('조회 기관명 입력: ')
    ggmap=gmaps.geocode(sch, language='ko')
    #ggmap

    print(ggmap[-1]['formatted_address'])
    print(ggmap[-1]['geometry']['location']['lat'])
    print(ggmap[-1]['geometry']['location']['lng'])


html_str = """
<html>
    <body>
        <ul class="greet">
            <li>hello</li>
            <li>bye</li>
            <li>welcome</li>
        </ul>
        <ul class="reply">
            <li>ok</li>
            <li>no</li>
            <li>sure</li>
        </ul>
        <div>
            <ul>
                <li>open</li>
                <li>close</li>
            </ul>
        </div>
    </body>
</html>
"""

# gglmaps()
soup=bs(html_str, 'html.parser')   # BeautifulSoup을 이용한 파싱(구조 초기화, 모든 태그 소문자 변경)
#print(soup)

# 출력물 1개
print(soup.find('ul'))   # find("태그") : 대상 개체에서 입력한 '태그'에 대한 첫번째 태그를 찾아 출력
print(soup.find('ul', class_="reply"))  # class_="reply" : 클래스/ID 와 같은 값으로 찾기
print(soup.find_all('ul'))  # soup.find_all('ul'): soup에 있는 모든 'ul' 태그 출력, 복수형 자료(리스트)

html_txt=soup.find_all('ul')
print(html_txt[-1])

# 태그를 제거하고 태그가 가지고 있는 텍스트 값 출력

print(html_txt[-1].text)        # text 속성값 출력
print(html_txt[-1].get_text())  # get_text() 함수 이용

html_txt=soup.find_all('li')
print(html_txt)

for txt in html_txt:
    print(txt.text)