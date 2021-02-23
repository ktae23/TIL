## CSS 속성

<br />

### 박스 속성

- 각 요소가 박스라는 사각 영역을 생성
- 해당 영역이나 둘러싼 테두리에 크기, 생상, 위치 등과 관련한 속성을 지정함으로써 스타일 변경 가능

<br />

| 속성 명      | 설명                                                         |
| ------------ | ------------------------------------------------------------ |
| margin 속성  | 테두리와 다른 태그 사이의 테두리 바깥쪽 여백                 |
| border 속성  | 테두리                                                       |
| padding 속성 | 테두리와 글자 사이의 테두리 안쪽 여백, 배경색은 padding 영역까지만 적용 |
| width 속성   | 글자를 감싸는 영역의 가로 크기                               |
| height 속성  | 글자를 감싸는 영역의 세로 크기                               |

[참고](https://www.w3schools.com/)

<br />

#### 박스 크기와 여백

- width, height
  - 글자를 감싸는 영역 크기 지정
- border
  - 테두리 두께 지정
- margin
  - 테두리와 다른 태그 간격 지정
  - margin-left
  - margin-right
  - margin-top
  - margin-bottom
- padding
  - 테두리와 글자 사이 간격 지정
  - padding-left
  - padding-right
  - padding-top
  - padding-bottom

<br />

![img](https://i2.wp.com/amati.io/wp-content/uploads/2019/04/box1_preserde.gif?resize=300%2C172&ssl=1)

<br />

- margin, border, padding 속성은 양쪽에 위치하기 때문에 전체 너비, 높이에 여백을 합해 구할 때는 2배 곱하여 더해 줘야 함

<br />

#### 박스 테두리

- 두께, 형태, 색상에 해당하는 속성을 사용하여 테두리 설정
- border-width
  - 테두리 두께
- border-style
  - 테두리 형태
- border-color
  - 테두리 색상

<br />

```css
<!DOCTYPE html>
<html>
<head>
    <title>박스 연습</title>
    <style>
        .box {
            border-width: thick;
            border-style: dashed;
            border-color: black;

            /* border-radius: 왼쪽위 오른쪽위 오른쪽아래 왼쪽아래 */
            border-radius: 50px 40px 20px 10px;
        }
    </style>
</head>
<body>
    <div class="box">
        <h1>박스</h1>
    </div>
</body>
</html>
```

<br />

### 가시 속성

- 태그가 화면에 보이는 방식 지정 ( 예 : display)
  - diplay를 사용해 박스를 인라인, 블록, 인라인-블록 형식으로 설정 가능
- 주로 사용하는 키워드
  - none, block, inline, inline-block

<br />

#### none

- display 속성에 적용 시 태그가 화면에서 보이지 않고 div 태그 전체가 사라짐

<br />

#### block

- #box 태그의 display 속성을 block으로 변경
- 블럭 단위로 영역 차지

<br />



#### inline

- #box 태그의 display 속성을 inline으로 변경
- 차지하는 글자수 단위로 영역 차지

<br />



#### inline-block

- #box 태그의 display 속성을 inline으로 변경
- 라인 안에서 블럭 단위로 영역 차지

<br />

```css
<style>
    #box { display : none;}
    #box { display : block;}
    #box { display : inline;}
	#box { display : inline-block;}
</style>
```

<br />

### 배경 속성

| 속성                  | 설명                         |
| --------------------- | ---------------------------- |
| background-image      | 배경 이미지 삽입             |
| background-size       | 배경 이미지의 크기 지정      |
| background-repeat     | 배경 이미지의 반복 형태 지정 |
| background-attachment | 배경 이미지의 부착 형태 지정 |
| background-position   | 배경 이미지의 위치 지정      |
| background            | 모든 배경 속성 한 번에 입력  |

<br />

```css
background-img : url('[이미지 경로]');
background-position : bottom;
background-size : 100%;
background-repeat : no-repeat;
background-attachment : fixed;
background-color : #1157FF;
/* 위를 한번에 입력하는 코드*/
background : url('[이미지 경로]') bottom 100% no-repeat fixed
```

<br />

### 글자 속성

#### 글자 크기와 글꼴

```css
<!DOCTYPE html>
<html>
<head>
    <title>글자 스타일 연습</title>
    <style>
		.font_arial { font-family: Arial;}	/*한 단어 폰트는 그냥 작성*/
		.font_roman { font-family: 'Time New Roman';}	/*여러 단어 폰트는 따옴표 처리*/
		.font_arial { font-family: 'Time New Roman', Arial;}	/*없는 폰트가 있으면 다른 폰트로 대체하여 조회*/
		.font_roman { font-family: '[없는 글꼴]',sans-serif;}	/*폰트가 없을 경우 산세리프 체 사용*/
		.a { font-size: 32px; }
        .b { font-size: 2em; }
        .c { font-size: large; }
        .d { font-size: small; }
    </style>
</head>
<body>
    <h1>Lorem ipsum</h1>
    <p class="a">글자 클래스 a</p>
    <p class="b">글자 클래스 b</p>
    <p class="c">글자 클래스 c</p>
    <p class="d">글자 클래스 d</p> 
</body>
</html>
```

<br />

#### 글자의 스타일과 두께

```css
    <style>
        .font_big { font-size: 2em; }
        .font_italic { font-style: italic; }
        .font_bold { font-weight: bold; }
    </style>
```

<br />

#### 글자 정렬

```css
    <style>
        .font_big { font-size: 2em; }
        .font_italic { font-style: italic; }
        .font_bold { font-weight: bold; }
        .font_center { text-align: center; }
        .font_right { text-align: right; }
    </style>
```

- span 태그는 인라인 형태로 너비의 개념이 없기 때문에 정렬이 안 됨
- div 태그는 블럭 형태로 수직 정렬을 지정 불가
  - 글자를 감싸는 박스의 높이와 같은 크기로 line-height 속성 지정하면 가능

<br />

#### 링크 글자 밑줄 제거

```css
    <style>
        a { text-decoration: none; }
    </style>
```

<br />

