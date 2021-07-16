import pandas as pd
import os
from datetime import datetime, timedelta
import urllib.request
import json


def main():
    ### 시작일 입력
    while True:
        sDate = input('시작일을 입력하세요.(예:20210401)')  # '20210301'
        if len(sDate) != 8:
            print('"20210401"과 같이 8자리로 입력해주세요')
            continue
        try:
            sd = int(sDate)
            break
        except:
            print('문자가 포함되어 있습니다. 다시 입력해주세요.')
            continue

    ### 종료일 입력
    while True:
        eDate = input('종료일을 입력하세요.(예:20210401)')  # '20210331'
        if len(eDate) != 8:
            print('"20210401"과 같이 8자리로 입력해주세요')
            continue
        try:
            sd = int(eDate)
            today = datetime.today()
            newDate = today.strftime('%Y%m%d')
            if sd >= int(newDate):
                print('데이터는 당일 이전 자료까지만 제공됩니다..\n다시 입력해주세요')
                continue

            if int(sDate) > sd:
                print('종료일이 시작일보다 이전 날짜입니다.\n다시 입력해주세요')
                continue
            break
        except:
            print('문자가 포함되어 있습니다. 다시 입력해주세요.')
            continue

    regionName = input('검색할 지점명을 입력하세요.')

    if regionName == "":
        return startWeatherData(sDate, eDate)
    else:
        return startWeatherData(sDate, eDate, regionName)


def startWeatherData(sDate, eDate, regionName='서울'):  # 데이터 처리

    pointID = getRegionalCode(regionName)  # 지점코드

    url = getRegionalUrl(sDate, eDate, pointID)  # 기본 url 생성
    # print(url)
    response = urllib.request.urlopen(url)  # url을 이용한 정보 요청
    rescode = response.getcode()  # 요청값 가져오기
    if rescode != 200:  # 요청값 정상 여부 확인
        raise Exception("Error Code:" + rescode)

    numRows = int(json.loads(response.read())['response']['body']['totalCount'])  # 전체 조회 데이터 개수 추출
    urlNew = getRegionalUrl(sDate, eDate, pointID, numRows)  # 전체 조회데이터 개수를 이용한 새로운 url 생성
    # print(urlNew)
    items = getJson(urlNew)  # 전체 데이터 url을 이용한 데이터 추출
    # print(items)
    return_df = getWeatherData(items)  # 가져온 데이터를 이용한 원하는 값 DataFrame 형식으로 출력

    saveWeatherData(sDate, eDate, regionName, return_df)  # 데이터 저장
    print('저장되었습니다.')

    return return_df


def saveWeatherData(sDate, eDate, regionName, return_df):
    filename = ""
    if sDate == eDate:
        filename = f"{regionName}_일별기상정보_{sDate}.csv"  # 시작일과 종료일이 같은 경우
    else:
        filename = f"{regionName}_일별기상정보_{sDate}_{eDate}.csv"  # 시작일과 종료일이 다른 경우

    return_df.to_csv('c:/pydata/' + filename, encoding='cp949', index=False)


def getWeatherData(items):
    tmpList = []
    for item in items:
        if item['sumRn']=="":
            sumRn=0
        else:
            sumRn=item['sumRn']
        tmpList.append([item['stnNm'], item['tm'], item['avgTa'], item['minTa'], item['maxTa'], sumRn])
    return pd.DataFrame(tmpList, columns=['지점명', '날짜', '평균기온', '최저기온', '최고기온', '일강수량'])

def getJson(urlNew):
    response = urllib.request.urlopen(urlNew)
    rescode = response.getcode()
    if(rescode==200):
        return json.loads(response.read())['response']['body']['items']['item']
    else:
        raise Exception("Error Code:" + rescode)

def getRegionalUrl(sDate, eDate, pointID, numRow=10):  # url 생성
    Key='VFYXI%2F3A5kIk7QVsO1epAlZSb6ZtCO9N5VC%2FMF3pW6oZ7%2BsFrvH1Y9y0UfmVzTnA0k2FasoNM6t6JsY7%2BEwNEw%3D%3D'
    url='http://apis.data.go.kr/1360000/AsosDalyInfoService/getWthrDataList?serviceKey='+Key
    url=url+'&dataType=JSON&numOfRows='+str(numRow)+'&pageNo=1&dataCd=ASOS&dateCd=DAY&startDt='+sDate
    url=url+'&endDt='+eDate+'&stnIds='+str(pointID)
    return url


def getRegionalCode(inputRegName):  # 검측 지점코드 취득

    df_RegCode = pd.read_csv('c:/pydata/지역코드.csv', encoding='cp949')
    regCode = df_RegCode[df_RegCode['지점명'] == inputRegName]

    if len(regCode) == 1:
        return int(regCode['지점'].values)
    else:
        # 사용자에 의해서 강제로 에러 발생
        raise Exception(f"해당 지역명이 없습니다.\n지역명을 확인하고 다시 입력해주세요\n\n{inputRegName}")

if __name__ == '__main__':
    df1=main()


