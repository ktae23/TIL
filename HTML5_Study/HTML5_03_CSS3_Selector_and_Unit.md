## CSS 선택자

- 특정 HTML 태그를 선택할 때 사용

<br />

### 기본 선택자

```css
h1[선택자] { 
    	color[스타일 속성] : red[스타일 값] ;
}
```

<br />

| 종류          | 형태            | 사용 예     |
| ------------- | --------------- | ----------- |
| 전체 선택자   | *               | *           |
| 태그 선택자   | 태그            | h1, p 등등  |
| 아이디 선택자 | #아이디         | #id         |
| 클래스 선택자 | .클래스         | .header     |
| 후손 선택자   | 선택자 선택자   | header h1   |
| 자손 선택자   | 선택자 > 선택자 | header > h1 |

[참고](https://www.w3schools.com/)

<br />

### 속성 선택자

- input 태그는 이름이 모두 같지만 type 속성에 따라 형태가 달라서 속성 선택자 많이 사용

```css
<!DOCTYPE html>
<html>
<head>
    <title>CSS3 Selector Basic Page</title>
    <style>
        input[type="text"] { background: red; }
        input[type="password"] { background: blue; }
    </style>
</head>
<body>
    <form>
        <input type="text" />
        <input type="password" />
    </form>
</body>
</html>
```

<br />

| 종류              | 설명                                           |
| ----------------- | ---------------------------------------------- |
| 선택자[속성]      | 특정한 속성이 있는 태그 선택                   |
| 선택자[속성 = 값] | 특정한 속성 내부 값이 특정 값과 같은 태그 선택 |

<br />

### 후손 선택자와 자손 선택자

<br />

![img](https://mblogthumb-phinf.pstatic.net/MjAxOTA1MTlfOTQg/MDAxNTU4MjUxNDc1OTI4.O4x_x83j54Oq7DlRcaAcbnhuiNHigtXyZKKUUWt3GbAg.E6suM7mMK5iMMbBN1-NTGc3prTy4Ohu5iZRmhFhMYrsg.PNG.zlatmgpdjtiq/%EC%9E%90%EC%86%90_%ED%9B%84%EC%86%90_%EC%84%A0%ED%83%9D%EC%9E%90.png?type=w800)

<br />

#### 후손 선택자

- 후손 선택자는 특정한 태그의 후손을 선택할 때 사용
  - 선택자 A 선택자 B == 선택자 A의 후손인 선택자 B 선택

```css
/*header의 후손인 h1과 일반 h2 선택*/
<style>
	#header h1, h2 { color : red;}
</style>

/*header의 후손인 h1과 header의 후손인 h2 선택*/
<style>
	#header h1, #header h2 { color : red;}
</style>
```

<br />

#### 자손 선택자

- 자손 선택자는 특정한 태그의 자손을 선택할 때 사용
  - 선택자 A > 선택자 B == 선택자 A의 자손인 선택자 B 선택
- 테이블 태그에서는 사용하지 않는 것이 좋음

```css
/*header의 자손인 h1 선택*/
<style>
	#header > h1 { color : red;}
</style>
```

<br />

### 반응 / 상태 / 구조 선택자

<br />

#### 반응 선택자

- 사용자 반응으로 생성되는 특정한 상태를 선택
  - :hover
    - 특정 태그위에 마우스 커서를 올린 상태
  - :active
    - 특정 태그를 마우스로 클릭한 상태

<br />

```css
<style>
	#header:hover > h1 { color : orange;}
	#header:active > h1 { color : red;}
</style>
```

<br />

#### 상태 선택자

- 입력 양식의 상태를 선택 할 때 사용
  - :checked 
    - 체크 상태의 input 태그 선택
    - 타입 속성이 checbox 또는 radio인 input 태그가 선택 된 상태
  - :focus
    - 포커스를 맞춘 input 태그 선택
    - 사용자가 바로 입력할 수 있도록 입력 양식에 포커스를 둔 상태
    - 웹 페이지 하나당 input 태그 하나에만 포커스를 둘 수 있음
  - :enabled
    - 사용 가능한 input 태그 선택
    - input 태그에 값을 입력 할 수 있는 상태
  - :disabled
    - 사용 불가능한 iniput 태그 선택
    - input 태그에 값을 입력 할 수 없는 상태

<br />

```css
<!DOCTYPE html>
<html>
<head>
    <title>CSS3 Selector Basic</title>
    <style>
        /* input 태그가 사용 가능할 경우에
           background-color 속성에 white 키워드를 적용. */
        input:enabled { background-color: white; }

        /* input 태그가 사용 불가능할 경우에
           background-color 속성에 gray 키워드를 적용. */
        input:disabled { background-color: gray; }

        /* input 태그에 초점이 맞추어진 경우에
           background-color 속성에 orange 키워드를 적용. */
        input:focus { background-color: orange; }
    </style>
</head>
<body>
    <h2>사용 가능</h2>
    <input />
    <h2>사용 불가능</h2>
    <input disabled="disabled"/>
</body>
</html>
```

<br />

#### 구조 선택자

- 특정한 위치에 있는 태그를 선택 할 때 사용
  - :first-child
    - 형제 관계에서 첫 번째로 등장하는 태그 선택
  - :last-child
    - 형제 관계에서 마지막으로 등장하는 태그 선택
  - :nth-child(수열)
    - 형제 관계에서 앞에서 수열 번째로 등장하는 태그 선택
  - :nth-last-child(수열)
    - 형제 관계에서 뒤에서 수열 번째로 등작하는 태그 선택

<br />

```css
<!DOCTYPE html>
<html>
<head>
    <title>CSS3 Selector Basic</title>
    <style>
        ul { overflow: hidden; }
        li {
            list-style: none;
            float:left; padding: 15px;
        }

        li:first-child { border-radius: 10px 0 0 10px; }
        li:last-child { border-radius: 0 10px 10px 0; }

        li:nth-child(2n) { background-color: #FF0003; }
        li:nth-child(2n+1) { background-color:#800000; }
    </style>
</head>
<body>
    <ul>
        <li>첫 번째</li>
        <li>두 번째</li>
        <li>세 번째</li>
        <li>네 번째</li>
        <li>다섯 번째</li>
        <li>여섯 번째</li>
        <li>일곱 번째</li>
    </ul>
</body>
</html>

```

<br />

![image-20210223174452781](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210223174452781.png)

<br />

## CSS 단위

- 스타일 값으로 입력 할 수 있는 단위에는 키워드, 크기, 색상, URL이 있음

<br />

### 키워드 단위

- W3C에서 미리 정의한 단위
- 키워드를 스타일 값으로 입력하면 해당하는 스타일이 자동 적용 됨
- 자동 완성을 지원함

<br />

### 크기 단위

- CSS에서 가장 많이 사용하는 단위
  - %(백분율), em(배수), cm, mm, inch, px이 있음

```css
<style>
    p:nth-child(1) { }
    p:nth-child(2) { font-size: 1.0em; }	/* 1배 = 100%*/
    p:nth-child(3) { font-size: 1.5em; }	/* 1.5배 = 150%*/
    p:nth-child(4) { font-size: 2.0em; }	/* 2배 = 200%*/
</style>
```

<br />

### 색상 단위

- 영어로 색상을 입력
- 또는 RGB 색상, RGBA색상, HEX 코드 단위 입력

<br />

##### RGB 색상

- 형태 
  - rgb(red, green, blue)
- 설명
  - R,G,B를 조합해 색상을 표현
  - 0~255 사이의 숫자를 입력

```css
<style>
	h1{bakgroud-clolr : rgb(255, 255, 255)}
</style>
```

<br />

##### RGBA 색상

- 형태 
  - rgba(red, green, blue, alpha)
- 설명
  - RGB 색상 단위에 알파 값을 추가
  - 알파 값은 투명도를 나타내며 0.0~1.0사이의 숫자를 입력
  - 투명(0.0) ~ 불투명(1.0)

```css
<style>
	h1{bakgroud-clolr : rgba(255, 255, 255, 0.5)}
</style>
```

<br />

##### HEX 코드

- 형태 
  - #000000 (6자리 수)
- 설명
  - RGB 색상 단위를 짧게 입력하는 방법으로 RGB 색상 조합을 16진수로 입력
  - #(빨강빨강)(초록초록)(파랑파랑)

```css
<style>
	h1{bakgroud-clolr : #0094FF;}
</style>
```

<br />

### URL 단위

- 이미지나 글꼴 파일을 불러 올때 사용
- 경로를 입력

```css
<style>
    body { 
            background-image : url('[이미지 ]')
    }
</style>
```



<br />

| 종류          | 형태            | 사용 예     |
| ------------- | --------------- | ----------- |
| 전체 선택자   | *               | *           |
| 태그 선택자   | 태그            | h1, p 등등  |
| 아이디 선택자 | #아이디         | #id         |
| 클래스 선택자 | .클래스         | .header     |
| 후손 선택자   | 선택자 선택자   | header h1   |
| 자손 선택자   | 선택자 > 선택자 | header > h1 |

<br />