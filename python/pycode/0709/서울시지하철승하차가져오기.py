import 서울시_지하철_승하차_현황_API as sw_pa
import 서울시유무임승차현황 as sw_py_fr
import pandas as pd
from datetime import datetime, timedelta

def Data_input_all():
    # 전체 데이터 API에서 가져오기
    sDt = "20150101"
    eDt = datetime.today().strftime('%Y%m%d')

    ### 지하철 승하차인원 데이터 조회 및 저장
    df0 = sw_pa.main_api(sDt, eDt)
    df0.to_csv('c:/pydata/seoul_sw_inout.csv', index=False)

    ### 지하철 유/무임승하차인원 데이터 조회 및 저장
    df1 = sw_py_fr.main_api(sDt[:-2], eDt[:-2])
    df1.to_csv('c:/pydata/seoul_sw_payfree.csv', index=False)
    print("===== API 데이터 가져오기 성공 =====")

def Data_update():

    df0 = pd.read_csv('c:/pydata/seoul_sw_inout.csv')
    df1 = pd.read_csv('c:/pydata/seoul_sw_payfree.csv')

    #### 일자별 승하차 데이터 업데이트
    if str(df0.iloc[-1, 0]) != datetime.today().strftime('%Y%m%d'):
        sDt = datetime.strptime(str(df0.iloc[-1, 0]), '%Y%m%d') + timedelta(1)
        sDt = sDt.strftime('%Y%m%d')
        eDt = datetime.today().strftime('%Y%m%d')

        df = sw_pa.main_api(sDt, eDt)

        df0 = pd.concat([df0, df], ignore_index=True)
        df0.to_csv('c:/pydata/seoul_sw_inout.csv', index=False)

    print('일자별 데이터 업데이트 완료')

    #### 년월별 유임/무임 승하차 데이터 업데이트
    sDt = datetime.strptime(str(df1.iloc[-1, 0]) + "27", '%Y%m%d') + timedelta(5)

    if sDt.strftime('%Y%m') != datetime.today().strftime('%Y%m'):
        sDt = sDt.strftime('%Y%m')
        eDt = datetime.today().strftime('%Y%m')
        df = sw_py_fr.main_api(sDt, eDt)
        df1 = pd.concat([df1, df], ignore_index=True)
        df1.to_csv('c:/pydata/seoul_sw_payfree.csv', index=False)

    print('유/무임 데이터 업데이트 완료')

    return df0, df1

# Data_input_all()
df0, df1 =Data_update()

#### 데이터 전처리
df0=df0.astype({'사용일':"str"})
print(df0.info())
df1=df1.rename(columns={'사용일':'사용년월'})
df1=df1.astype({'사용년월':'str'})
print(df1.info())

