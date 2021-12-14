## 구분자 없는 문자열을 배열로 변환 

그럴일은 잘 없지만 인덱싱을 위해 구분자 없이 이어진 문자열을 뜯어 배열로 만들어야 할 때가 있다.

구분자가 있다면 split을 사용하는게 간단하고 확실하지만 없다면 직접 구현하는 편이 좋다.

 직접 구현하면 구분자가 없기 때문에 1글자, 2글자, 3글자 등 정해진 길이만큼만 잘라낼 수 있다는 특징이 있다.

<br />

#### 구분자 없는 split 구현 예제

```java
    private String[] splitStr(String str) {
        String[] strArray = new String[str.length()];

        for (int i = 0; i < str.length(); i++) {
            if (i == str.length() - 1) {
                strArray[i] = str.substring(i);
                break;
            }
            strArray[i] = str.substring(i, i + 1);
        }
        return strArray;
    }
```

문자열 자르기를 위해 Java.Lang.String.substring 함수를 사용한다.

substring은 split에 비하면 단순한 구조이지만 직접 구현해서 쓰기엔 비효율적인.. 딱 그 정도인것 같다.

<br />

사실 한 글자씩 뜯을 목적이라면 split함수를 아래와 같이 사용하면 같은 결과를 반환하기 때문에 간편하다.

```java
String[] resultArray = str.split("");
```

<br />

하지만 split함수는 정규표현식을 사용하기에 한번 로직을 따라 스텝을 밟아보면 굉장히 많은 단계를 거치는것을 알 수 있다. 

내가 모든 함수, 모든 로직에서 효율이나 자원관리를 생각하지는 못하지만 아는 내용 안에서는 그러려고 노력한다.

<br />

때문에 특정한 목적이 있는 간단한 함수는 직접 구현하는 편이 좋다.

<br />

<hr />

<br />

#### split을 이용한 방식과 직접 구현한 방식 비교

<br />

##### 1. split 함수 사용

```java
Split split = new Split();
String[] result = split.splitStr("OXOOXOXOXOXOXXOOXOXOXOXOXXOXOX");

long beforeTime = System.currentTimeMillis();
System.out.println("이전 : " + beforeTime);

System.out.println(Arrays.toString(result));

long afterTime = System.currentTimeMillis();
System.out.println("이후 : " + afterTime);

System.out.println("시간차이 : " + (afterTime - beforeTime));
```

<br />

##### 출력 결과

```shell
이전 : 1636265895774
[O, X, O, O, X, O, X, O, X, O, X, O, X, X, O, O, X, O, X, O, X, O, X, O, X, X, O, X, O, X]
이후 : 1636265895786
시간차이 : 12
```

<br />

##### 2. 직접 구현

```java
Split split = new Split();
String test = "OXOOXOXOXOXOXXOOXOXOXOXOXXOXOX";

long beforeTime = System.currentTimeMillis();
System.out.println("이전 : " + beforeTime);

String[] result = test.split("");
System.out.println(Arrays.toString(result));

long afterTime = System.currentTimeMillis();
System.out.println("이후 : " + afterTime);

System.out.println("시간차이 : " + (afterTime - beforeTime));
```

<br />

##### 출력 결과

```shell
이전 : 1636265976195
[O, X, O, O, X, O, X, O, X, O, X, O, X, X, O, O, X, O, X, O, X, O, X, O, X, X, O, X, O, X]
이후 : 1636265976209
시간차이 : 14
```

<br />

<hr />

매우 단순하고 테스트라고 할 수도 없지만.. 어쩌면 당연하게도 저 짧고 단순한 문자열을 뜯는데도 split 함수를 사용하는 편이 시간이 조금 더 걸리는 것을 알 수 있다.

<br />

되도록 같은 결과를 내더라도 불필요한 로직이 담긴 함수를 사용하기 보단 목적에 맞는 함수를 구현하는 편이 좋다.