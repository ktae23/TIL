import Modal_Class as MC
from Modal_Class import FourCal

### 클래스 상속
class FourCal_chr(FourCal):
    def __init__(self, number):
        super().__init__()
        self.number=number   # self.number=10

    def add_2(self):      #number=20
        prn_number = self.number + self.result
        return prn_number



obj1=FourCal()           # FourCal.result=0
print(obj1.add(10))      # FourCal.result=10
print(obj1.add(10))      # FourCal.result=20

obj2=FourCal()           # FourCal.result=0
print(obj2.add(20))      # FourCal.result=20
print(obj2.add(20))      # FourCal.result=40

obj3=FourCal_chr(10)     # FourCal.result=0, FourCal_chr.number=10
obj3.add(10)             # FourCal.result=10, FourCal_chr.number=10
prn_number=obj3.add_2()  # FourCal.result=10 + FourCal_chr.number=10 => prn_number
print(obj3.result, obj3.number, prn_number)  # FourCal.result=10, FourCal_chr.number=10, prn_number=20






