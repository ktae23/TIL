import matplotlib.pyplot as plt
import numpy as np

lst_data1=[10, 35, 27, 33, 48, 52, 18, 25, 27, 45]
lst_data2=list(np.random.randint(10, 50, size=10))

print(lst_data2)

### 여러개 차트 한 화면에 출력(두개의 행으로 출력)
fig=plt.figure()

ax1=fig.add_subplot(2,1,1)    # add_subplot(행 개수, 열 개수, 출력 위치)
ax2=fig.add_subplot(2,1,2)

ax1.plot(lst_data1, 'k')
ax2.plot(lst_data2, 'r')

plt.show()

### 여러개 차트 한 화면에 출력(두개의 열으로 출력)
fig=plt.figure(figsize=(12, 3))

ax1=fig.add_subplot(1,2,1)
ax2=fig.add_subplot(1,2,2)

ax1.plot(lst_data1, 'k')
ax2.plot(lst_data2, 'r')

plt.show()


### 여러개 차트 한 화면에 출력(행/열 여러개 출력하기)
fig=plt.figure(figsize=(12, 8))   # 차트 화면 사이즈 설정

ax1=fig.add_subplot(3,2,1)
ax2=fig.add_subplot(3,2,2)
ax3=fig.add_subplot(3,1,2)
ax4=fig.add_subplot(3,3,7)
ax5=fig.add_subplot(3,3,8)
ax6=fig.add_subplot(3,3,9)

ax1.plot(lst_data1, 'k')
ax2.plot(lst_data2, 'r')
ax3.plot(lst_data1)
ax4.pie(lst_data2)

plt.show()

fig, ax_lst=plt.subplots(2, 2, figsize=(8,8))

ax_lst[0][0].plot(lst_data1)
ax_lst[1][1].plot(lst_data2)

plt.show()