import re
import os
import requests
from bs4 import BeautifulSoup as bs
import openpyxl
from urllib.request import urlretrieve

'''
(참고) 영화관련정보 엑셀(xlsx)형식 저장 컬럼 목록
    1) 영화제목
    2) 영화평점
    3) 영화장르
    4) 영화감독
    5) 영화배우
    6) 영화줄거리
    7) 영화포스터
    8) 영화코드번호
'''
'''
 코드 작성자
 박용태
 https://www.coalastudy.com/feed/1516

 코드 수정 사용
 2021-04-08
 박경태
 https://ktae23.tistory.com/
  
 수정 내용
 1. 영화 포스터 저장 시 파일명에 특수문자 제거를 위한 정규표현식 사용
 2. 평점이 없는 영화, 청소년 관람 불가 영화 건너뛰기
 3. 영화 줄거리 가져오기
 4. 영화 포스터를 실제로 가져온 영화 정보 순서에 맞게 행에 입력하기
 5. 이미지 파일을 imgs 디렉토리에 저장하기
 
'''

# 특수문자 제거 위한 함수
def cleanText(readData):
    # 텍스트에 포함되어 있는 특수 문자 제거
    text = re.sub('[-=+,#/\?:^$.@*\"※~&%ㆍ!』\\‘|\(\)\[\]\<\>`\'…》]', '', readData)
    return text


def crawling(start_code, finish_code):
    wb = openpyxl.Workbook()
    sheet = wb.active
    sheet.append(["영화제목", "영화평점", "영화장르", "영화감독", "영화배우", "영화줄거리", "영화코드번호", "영화포스터"])

    # (0) HTML 파싱
    # 저장 된 영화와 포스터의 행을 맞추기 위한 정수 j
    j = 0
    # 청소년 관람 불가, 평점 없는 영화 제외
    # 네이버영화의 영화 코드 범위 지정
    for i in range(start_code, finish_code):

        movie_code = str(i)
        raw = requests.get("https://movie.naver.com/movie/bi/mi/basic.nhn?code=" + movie_code)
        html = bs(raw.text, 'html.parser')

        # (1) 전체 컨테이너
        movie = html.select("div.article")

        # (2) 전체 컨테이너가 갖고 있는 영화관련 정보
        for a, m in enumerate(movie):

            # (3-1) 영화제목 수집
            title = m.select_one("h3.h_movie a")

            # (3-2) 영화평점 수집
            score = m.select_one("div.main_score div.score a div.star_score span.st_off span.st_on")

            '''
               (참고) select + nth-of-type 문법 활용
                   -> select_one 아니라, select를 써야 여러개 혹은 여러명의 영화장르/영화감독/영화배우/줄거리 리스트를 가져 오게 됩니다.
            '''

            # (3-3) 영화장르 수집
            genre = m.select("dl.info_spec dd p span:nth-of-type(1) a")

            # (3-4) 영화감독 수집
            directors = m.select("dl.info_spec dd:nth-of-type(2) a")

            # (3-5) 영화배우 수집
            actors = m.select("dl.info_spec dd p:nth-of-type(3) a")

            # (3-6) 영화줄거리 수집
            story = m.select("div.story_area p.con_tx")

            '''
               (참고) 고급 검색 활용
                   -> if/else 문을 이용한 여러가지 명제들을 활용하면, 사용자가 임의로 원하는 데이터만 필터링 할 수 있습니다.
            '''
            # (4) skip 처리-1: 평점이 없으면 넘어간다.
            non_score = "관람객 평점 없음"
            if (score == None) or (non_score in score.text):
                is_ok = False
                continue

            # (5) skip 처리-2 : 주연배우에 "청소년 관람불가"가 포함되어 있으면 넘어간다.
            invalid = "청소년 관람불가"
            if invalid in actors[0].text:
                is_ok = False
                continue

            '''
               (참고) Standard Output(일반 출력)
                   -> 출력을 보기 쉽게 만들어주는 것은 데이터 수집 확인용을 위해 중요합니다.
            '''
            # (6) ~~~~~ 이쁘게 출력 ~~~~~~~
            print("=" * 50)
            print("제목:", title.text)

            print("=" * 50)
            if score != None:
                print("평점:", score.text)

            print("=" * 50)
            print("장르:")
            for g in genre:
                print(g.text)

            print("=" * 50)
            print("감독:")
            for d in directors:
                print(d.text)

            print("=" * 50)
            print("주연 배우:")
            for a in actors:
                print(a.text)

            print("=" * 50)
            print("줄거리:")
            for s in story:
                print(s.text)

            print(j)

            # (7) 영화관련정보 엑셀(xlsx) 형식 저장
            # (7-1) 데이터 만들기-1 : HTML로 가져온 영화장르/영화감독/영화배우 정보에서 TEXT정보만 뽑아서 리스트 형태로 만들기
            genre_list = [g.text for g in genre]
            directors_list = [d.text for d in directors]
            actors_list = [a.text for a in actors]
            story_list = [s.text for s in story]

            # (7-2) 데이터 만들기-2 : 여러 개로 이루어진 리스트 형태를 하나의 문자열 형태로 만들기
            genre_str = ','.join(genre_list)
            directors_str = ','.join(directors_list)
            actors_str = ','.join(actors_list)
            story_str = ','.join(story_list)

            # (7-3) 영화관련정보 엑셀 행 추가 : line by line 으로 추가하기
            sheet.append([title.text, score.text, genre_str, directors_str, actors_str, story_str, movie_code])

            '''
               (참고) 영화 포스터 이미지 저장
                   -> 선택 사항 ^0^
            '''
            # (8) 영화포스터 수집
            img_src = m.select_one("div.poster a img")

            # (8-1) 영화제목 특수문자 제거, 공백 제거, : 변경
            title_rename = title.text.replace(" ", "").replace(":", "_")
            title_rename = cleanText(str(title_rename))

            # (8-2) 영화포스터 이미지파일 저장
            path = '../crawling/imgs/'
            urlretrieve(img_src.attrs["src"], path + title_rename + ".png")

            # (8-3) 영화포스터 이미지파일을 엑셀로 불러들이기

            img = openpyxl.drawing.image.Image(path + title_rename + ".png")
            img.width = 80
            img.height = 80

            # (8-4) 영화포스터 엑셀 행 추가 : 영화관련정보 옆(=G열)에 추가하기

            sheet.add_image(img, 'H' + str(j + 2))

            print("=" * 50)
            print(title_rename, "포스터 저장 완료!")
            is_ok = True
        if is_ok == True:
            j = j + 1
    # (9) 엑셀 저장
    wb.save("navermovie.xlsx")
