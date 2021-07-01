## Big - O 표기법

### [출처 : 엔지니어대한민국 유튜브 채널](https://www.youtube.com/watch?v=6Iq5iMCVsXA)

- 알고리즘의 성능을 수학적으로 표현하는 표기법
- 알고리즘의 시간, 공간 복잡도를 표기 가능
- 데이터나 사용자 증가에 따라 발생하는 알고리즘의 성능에 대해 표현

<br/>

### O(1) 

```c
F(int[] n){
    return (n[0] == 0) ? true:false;
}
```

- 어떤 값이 들어 오든 언제나 일정한 속도로 처리 되는 알고리즘

<br/>

### O(n)

```c
F(int[] n){
    for i = 0 to n.length
        print i
}
```



- 데이터가 증가 함에 따라 처리시간이 비례하여 증가하는 알고리즘

<br/>

### O(n^2)

```c
F(int[] n) {
    for i = 0 to n.length
        for j = 0 to n.length
            print i + j;
}
```

- 데이터가 늘어날 수록 n^2만큼 처리 시간이 증가하는 알고리즘

<br/>

### O(nm)

```c
F(int [] n, int[] m) {
    for i = 0 to n.length
        for j = 0 to m.length
            pring i + j;
}
```

- 변수가 다를 경우 반드시 n과 m으로 다르게 표시해야 함

<br/>

### O(n^3)

```c
F(int [] n) {
    for i = 0 to n.length
        for j = 0 to n.length
            for k = 0 to n.length
                print i + j + k;
}
```

- 데이터가 늘어날 수록 n^3만큼 처리 시간이 증가

<br/>

### O(2^n)

```c
F(n,r){
	if(n <= 0) return 0;
    else if (n == 1) return r[n] = 1;
    return r[n] = F(n-1, r) + F(n-2, r);
}
```

- 여러차례 호출하는 만큼 데이터에 따른 처리 시간 증가율이 매우 가파름

- 피보나치 수열

<br/>

### O(m^n)

- M개씩 n번 반복 되는 경우

```c
F(int [] n, int [] m) {
    for i = 0 to n.length
        for j = 0 to m.length
                print i + j;
}
```



<br/>

### O(log n)

```c
F(k, arr, s, e) {
	if(s>e) return -1;
    m = (s+e) / 2;
    if (arr[m] == k) return m;
    else if (arr[m] > k) return F(k, arr, s, m-1);
    else return F(k, arr, m+1, e);
}
```

- binary search가 대표적
  - 중간을 찾아 비교하여 앞, 뒤 중 하나만 검색하는 것을 반복하는 방식
- 검색 할 때마다 데이터가 절반씩 줄어드는 알고리즘\

<br/>

### O(sqrt(n)) 

- O(squre root(n))
- 사각형의 맨 위 네개(제곱근)를 구하는 알고리즘

![image-20210607224059814](C:\Users\zz238\TIL\Algorithm\img\sqrt)

<br/>

- Big O에서는 상수를 버린다.
  - O(2n) -> O(n)
  - Big O 표기법은 실제 알고리즘의 성능 테스트를 하기 위함이 아니라 장기적으로 데이터의 증가에 따른 처리시간의 증가율을 예측하기 위해 만들어진 표기법이기 때문
  - O(n^2 + n^2) -> O(n^2)
    - 차원이 달라지지 않는 한 동일하게 적용

<br/>



