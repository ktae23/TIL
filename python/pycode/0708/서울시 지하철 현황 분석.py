import 서울시_지하철_승하차_현황_API as sw_pa
import 서울시유무임승차현황 as sw_py_fr

sDt=input('시작일 입력(예: 20210101): ')
eDt=input('종료일 입력(예: 20210101): ')

df0=sw_pa.main_api(sDt, eDt)
df1=sw_py_fr.main_api(sDt, eDt)
print(df0)
print(df1)

