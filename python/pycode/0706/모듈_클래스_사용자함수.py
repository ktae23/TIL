import numpy.random

class Fun_cls:
    ### 사용자 정의 함수 생성(실행함수)
    ### 인스턴스 객체
    def usr_fun1(x, y, z=10):    # x, y, z 3개의 인수를 가지는 함수, z는 기본으로 10이 입력
        tot=x+y+z
        print(f'x={x}, y={y}, z={z}, 합계={tot}')

    ### 사용자 정의 함수 생성(계산 결과를 돌려주는 함수)
    def usr_fun2(x, y, z):
        tot = x + y + z         # x, y, z, tot 변수는 def 함수 안에서만 사용하는 지역변수
        p_tot=tot*10
        return tot, p_tot


# numpy.random.randint()

Fun_cls.usr_fun1(10, 10)      # def 정의된 함수 호출, z값 생략=>기본값 10 입력
Fun_cls.usr_fun1(20, 20, 20)  # def 정의된 함수 호출, z값20=>기본값 대신 z에 20이 들어감

fl=Fun_cls

tot, p_tot = fl.usr_fun2(10, 10, 10)
print(f'tot={tot}, p_tot={p_tot}')





