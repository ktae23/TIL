## 함수(메서드)만 가지고 클래스
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

## 사칙연산
class FourCal:
    def __init__(self):  # 생성자
       self.result=0     # FourCal 속성

    def add(self, num):  # 함수/인스턴스/메서스
        self.result = self.result + num
        return self.result

    def sub(self, num):
        self.result=self.result-num
        return self.result

    def mul(self, num):
        self.result=self.result*num
        return self.result

    def Div(self, num1):
        self.result=self.result/num1
        return self.result



if __name__ == "__main__":

    fcal1 = FourCal()   # FourCal 클래스를 이용해 fcal1 객체 생성
    print('FourCal 클래스 result=',fcal1.result)
    fcal1.add(10)
    print('FourCal.add 계산후 result =',fcal1.result)
    fcal1.sub(5)
    print('FourCal.sub 계산후 result =',fcal1.result)

    fcal1 = FourCal()
    fcal2 = FourCal()

    fcal1.add(10)
    print('FourCal 클래스 result=',fcal1.result)
    print('FourCal 클래스 result=',fcal2.result)
