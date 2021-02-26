## HTML 기본 용어

```html
<!DOCTYPE html>	<!-- 웹 브라우저에 HTML5 문서라는 것을 알리기 위해 반드시 첫 행에 나와야 함-->
<html>	<!--모든 html 페이지의 기본 요소, 모든 태그는 html태그 내에 작성-->
    <head><!--body 태그에 필요한 스타일 시트와 자바스크립트 제공-->
        <title>Hello HTML5</title><!--웹 브라우저에 표시하는 제목-->
    </head>
    <body><!--사용자에게 실제로 보여지는 부분 작성-->
        
    </body>
</html>

```

- 타이포라 코드 블럭 html로 설정하니까 자동으로 끝태그랑 들여쓰기를 제공한다. 너무 좋다 미쳤다.

<br />

##### 언어(lang)태그와 head에 넣는 태그

```html
<!DOCTYPE html>
<html> 
    <head lang="ko"> 
    <meta charset="utf-8"> 
    <title></title> 
    </head> 
    
    <body> 
    </body> 
</html>

```

| lang 속성 값 | 언어     |
| ------------ | -------- |
| ko           | 한국어   |
| en           | 영어     |
| ja           | 일본어   |
| ru           | 러시아어 |
| zh           | 중국어   |
| de           | 독일어   |

<br />

| 태그   | 설명                         |
| ------ | ---------------------------- |
| meta   | 웹 페이지에 추가 정보 전달   |
| title  | 페이지 제목 지정             |
| script | 웹 페이지에 스크립트 추가    |
| link   | 웹 페이지에 다른 파일 추가   |
| style  | 웹 페이지에 스타일 시트 추가 |
| base   | 웹 페이지의 기본 경로 지정   |

<br />

## HTML 기본 태그

##### 글자 태그

```html
<!DOCTYPE html>
<html>
<head>
    <title>HTML5 + CSS3 Text</title>
</head>
<body>
    <h1>제목 글자 태그 1</h1>
    <h2>제목 글자 태그 2</h2>
    <h3>제목 글자 태그 3</h3>
    <h4>제목 글자 태그 4</h4>
    <h5>제목 글자 태그 5</h5>
    <h6>제목 글자 태그 6</h6>
</body>
</html>

```

| 태그      |      | 설명                          |
| --------- | ---- | ----------------------------- |
| 제목 글자 | h1   | 첫 번째로 큰 제목 글자 생성   |
|           | h2   | 두 번째로 큰 제목 글자 생성   |
|           | h3   | 세 번째로 큰 제목 글자 생성   |
|           | h4   | 네 번째로 큰 제목 글자 생성   |
|           | h5   | 다섯 번째로 큰 제목 글자 생성 |
|           | h6   | 여섯 번째로 큰 제목 글자 생성 |
| 본문 글자 | p    | 본문 문단 생성                |
|           | br   | 줄 바꿈                       |
|           | hr   | 수평 줄 삽입                  |

<br />

##### 특수 문자 표기

```html
<!DOCTYPE html>
<html>
<head>
    <title>HTML5 + CSS3 Text</title>
</head>
<body>
    <h1>공백이&nbsp;&nbsp;&nbsp;있는&nbsp;&nbsp;&nbsp;글자</h1>
    <p>
        1 은 2보다 작다 == 1 &lt; 2 <br />
        2 는 1보다 크다 == 2 &gt; 1 <br />
        1 그리고 2 == 1 &amp; 2 <br />
        쌍 따옴표 &quot; <br />
    </p>
</body>
</html>

```

| 특수 문자 | 출력 문자 | 의미                        |
| --------- | --------- | --------------------------- |
| `&nbsp;`  | 공백      | non - breaking space (공백) |
| `&lt;`    | <         | Less than sign (작다)       |
| `&gt;`    | >         | Greater than sign (크다)    |
| `&amp;`   | &         | Ampersand (&)               |
| `&quot;`  | "         | quotation                   |

<br />

![image-20210222235714857](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210222235714857.png)

[특수 문자 참고 블로그](https://sensechef.com/957)

<br />

##### 앵커 태그

```html
<!DOCTYPE html>
<html>
<head>
    <title>앵커 태그 연습</title>
</head>
<body>
    <a href="http://naver.com/">네이버</a><br />
    <a href="http://daum.com/">다음</a><br />
    <a href="#part1">part1 부분</a>
    <a href="#part2">part2 부분</a>
    <a href="#part3">part3 부분</a>
    <a href="#">링크 없는 부분</a>
    <hr />
    <h1 id="part1">part1</h1>
    <p>
        두 번째 파트 설명
    </p>
    <h1 id="part2">part2</h1>
    <p>
        두 번째 파트 설명
    </p>
    <h1 id="part3">part3</h1>
    <p> 
        세 번째 파트 설명
    </p>
</body>
</html>

```

<br />

##### 글자 태그

```html
<!DOCTYPE html>
<html>
<head>
    <title>글자 태그 연습</title>
</head>
<body>
    <h1><b>굵은 글자</b></h1>
    <h1><i>기울어진 글자</i></h1>
    <h1><small>작은 글자</small></h1>
    <h1>일반 글자 <sub> 아래 첨자</sub></h1>
    <h1>일반 글자 <sup> 위 첨자</sup></h1>
    <h1><ins>밑줄 글자</ins></h1>
    <h1><del>취소선 글자</del></h1>
    <hr />
    <b>굵은 글자</b><br />
    <i>울어진 글자</i><br />
    <small>작은 글자</small><br />
    일반 글자 <sub> 아래 첨자</sub><br />
    일반 글자 <sup> 위 첨자</sup><br />
    <ins>밑줄 글자</ins><br />
    <del>취소선 글자</del><br />
</body>
</html>
```

| 태그  | 설명                          |
| ----- | ----------------------------- |
| b     | 굵은 글자 (bold)              |
| i     | 기울어진 글자 (italic)        |
| small | 작은 글자                     |
| sub   | 아래 첨자 (subscript)         |
| sup   | 위 첨자 (superscript)         |
| ins   | 밑줄 글자 (insert)            |
| del   | 취소선이 그어진 글자 (delete) |

<br />

## 목록 태그

##### 

```html
<!DOCTYPE html>
<html>
<head>
    <title>목록 태그 연습</title>
</head>
<body>
  <ul><!-- 순서가 없는 목록 -->
      <!-- 첫번째 목록 -->
    <li>
      <b>분류</b><!-- 순서가 없는 목록 적용 대상-->
      <ol><!-- 순서가 있는 목록 -->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
      </ol>
    </li>
   <!-- 두번째 목록 -->
    <li>
      <b>분류</b><!-- 순서가 없는 목록 적용 대상-->
      <ol><!-- 순서가 있는 목록 -->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
        <li>목록</li><!-- 순서가 있는 목록 적용 대상-->
      </ol>
    </li>
  </ul>
</body>
</html>

```

| 태그 | 설명                                  |
| ---- | ------------------------------------- |
| ul   | 순서가 없는 목록 (unorderd list) 생성 |
| ol   | 순서가 있는 목록 (orderd list)생성    |
| li   | 목록 요소 (list item) 생성            |

<br />

## 테이블 태그

##### 

```html
<!DOCTYPE html>
<html>
<head>
    <title>테이블 태그 연습</title>
</head>
<body>
    <table border="1"><!--테두리 두께 1-->
        <tr>
            <th colspan="2">대분류</th><!-- 셀 너비 2칸 -->
        </tr>
        <tr>
            <th rowspan="3">중분류</th><!-- 셀 높이 3칸 -->
            <td>내용</td>
        </tr>
        <tr><td>내용</td></tr>
        <tr><td>내용</td></tr>
        <tr>
            <th rowspan="4">중분류</th><!-- 셀 높이 3칸 -->
            <td>내용</td>
        </tr>
        <tr><td>내용</td></tr>
        <tr><td>내용</td></tr>
        <tr><td>내용</td></tr>
    </table>
</body>
</html>

```

| 태그  | 설명                              |
| ----- | --------------------------------- |
| table | 표 삽입                           |
| tr    | 표에 행 (table row) 삽입          |
| th    | 표의 제목 셀 (table heading) 생성 |
| td    | 표의 일반 셀행 (table data) 생성  |

<br />

| 태그  | 속성    | 설명                  |
| ----- | ------- | --------------------- |
| table | border  | 표의 테두리 두께 지정 |
| th,td | colspan | 셀의 너비 지정        |
| th,td | rowspan | 셀의 높이 지정        |

<br />

## 미디어 태그

##### 

```html
<!DOCTYPE html>
<html>
<head>
    <title>미디어 태그 연습</title>
</head>
<body>
    <audio src="[오디오 경로]" controls="controls"></audio>
    <img src="[이미지 경로]" alt="기본 메시지" width="200" />
    <video width="600" controls="controls" poster="[준비 중 이미지 경로]">
        <source src="[비디오 경로]" type="video/mp4" />
    </video>
</body>
</html>

```

| 내용물을 가질 수 있는 태그                                   | 내용물을 가질 수 없는 태그 |
| ------------------------------------------------------------ | -------------------------- |
| ```<audio>```  ```</audio>```<br />```<video>``` ```</video>``` | ```<img>```                |

<br />

| 태그              | 속성     | 설명                                                       |
| ----------------- | -------- | ---------------------------------------------------------- |
| img 태그          | src      | 이미지 경로 지정                                           |
|                   | alt      | 이미지가 없을 때 나오는 글자 지정                          |
|                   | width    | 이미지 너비 지정                                           |
|                   | height   | 이미지 높이 지정                                           |
| audio, video 태그 | src      | 음악, 비디오 파일 경로 지정                                |
|                   | preload  | 음악, 비디오 준비 중일때 데이터를 모두 불러 올지 여부 지정 |
|                   | autoplay | 음악, 비디오의 자동 재생 여부 지정                         |
|                   | loop     | 음악, 비디오의 반복 여부 지정                              |
|                   | controls | 음악, 비디오 재생 도구 출력 여부 지정                      |
| video 태그        | width    | 비디오 너비 지정                                           |
|                   | height   | 비디오 높이 지정                                           |

