## 문서 객체 모델

- 문서 객체 (Document Object)
  - HTML 태그(h1, p 등)를 자바스크립트에서 사용 할 수 있는 객체로 만든 것
  - 문서 객체 조작은 HTML 태그 조작과 같은 의미
- 웹 브라우저는 HTML 페이지를 읽으면서 태그의 포함 관계에 따라 문서 객체를 트리 형태로 만듬
- 노드 (Node)
  - 트리의 각 요소
  - 요소 노드(Element node)
    - h1 태그, script 태그 등 요소를 생성하는 노드
  - 텍스트 노드(text node)
    - 화면에 출력되는 문자열
    - h1처럼 텍스트 노드를 갖는 태그도 있지만 br, hr, img 등 없는 태그도 있음

![img](https://eloquentjavascript.net/1st_edition/img/html.png)

<br />

### DOM

웹 브라우저는 웹 페이지를 실행할 때 먼저 HTML 파일을 분석한 후 화면에 표시한다.

웹 브라우저가 HTML 파일을 분석하고 출력하는 방식을 문서 객체 모델(Document Object Model)이라고 한다.

- 정적 생성
  - 웹 페이지를 처음 실행할 때 HTML 태그로 적힌 문서 객체를 생성하는 것
- 동적 생성
  - 웹 페이지를 실행 중에 자바스크립트를 사용해 문서 객체를 생성하는 것

<br />

### 웹 페이지 실행 순서

- 웹 브라우저는 HTML 코드를 위에서 아래 순서로 실행 한다.
  - 따라서 순서대로 작성하거나 이벤트 기능을 이용해야 한다.

<br />

## 문서 객체 선택

- 이미 존재하는 HTML 태그를 자바스크립트에서 문서 객체로 변환하는 것을 '문서 객체를 선택한다'라고 표현

<br />

##### 문서 객체 선택 메서드

| 구분         | 메서드                                 | 설명                        |
| ------------ | -------------------------------------- | --------------------------- |
| 1개 선택     | document.getElementByid(아이디)        | 아이디로 1개 선택           |
| 1개 선택     | document.querySelector(선택자)         | 선택자로 1개 선택           |
| 여러 개 선택 | document.getElementByName(이름)        | name 속성으로 여러 개 선택  |
| 여러 개 선택 | document.getElementByClassName(클래스) | class 속성으로 여러 개 선택 |
| 여러 개 선택 | document.querySelectorAll(선택자)      | 선택자로 여러 개 선택       |

- getTelementByid(아이디) 
  - 메서드 사용 시 id 속성이 중복되면 객체 선택 시 문제가 발생함
- querySelector(선택자)
  - 문서 객체 하나를 선택하기 때문에 같은 선택자가 여러개면 가장 먼저 등장하는 것 선택
- querySelectorAll(선택자)
  - 문서 객체 여러개 선택하여 배열로 반환
  - 모든 객체의 속성을 조작 할 경우 반복문을 함께 사용 함

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>DOM Basic</title>
    <script>
        // 이벤트를 연결합니다.
        window.onload = function () {
            // 문서 객체를 선택합니다.
            var headers = document.querySelectorAll('h1');

            for (var i = 0; i < headers.length; i++) {
                // 변수를 선언합니다.
                var header = headers[i];

                // 문서 객체를 조작합니다.
                header.style.color = 'orange';
                header.style.background = 'red';
                header.innerHTML = 'From JavaScript';
                }
            
            var header = document.querySelector('h2');

	            // 문서 객체를 조작합니다.
    	        header.style.color = 'yellow';
        	    header.style.background = 'green';
            	header.innerHTML = 'From JavaScript';
                
            var header = document.getElementById('header');

            // 문서 객체를 조작합니다.
            header.style.color = 'black';
            header.style.background = 'gray';
            header.innerHTML = 'From JavaScript';
        };
            
    </script>
</head>
<body>
    <h1>Header</h1>
    <h2>Header</h2>
    <h3 id="header">Header</h3>
</body>
</html>
```

![image-20210225002836254](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210225002836254.png)

<br />

## 문서 객체 조작

##### SPA(Single Page Application)

- 처음 웹 페이지를 읽어 들일 때 한 번만 읽어 들임
  - 처음에 틀만 읽고 이후에 자바스크립트 문서 객체를 조작하여 내용을 후입력

<br />

### 글자 조작

| 속성        | 설명                                                       |
| ----------- | ---------------------------------------------------------- |
| textContent | 문서 객체 내부 글자를 순수 텍스트 형식으로 가져오도록 변경 |
| innerHTML   | 문서 객체 내부 글자의 HTML 태그를 반영해 가져오도록 변경   |

- textContent를 사용한 경우
  - `<h1>Header - 0</h1><h2>Header - 1</h2>`

- innerHTML을 사용한 경우

  - <h1>Header - 0</h1><h2>Header - 1</h2>

    

<br />

### 스타일 조작

- 자바스크립트로 CSS 속성 값을 추가, 제거, 변경 가능
  - 문서 객체의 style 속성을 변경하면 됨
  - 자바스크립트에서는 특수 문자 `-` 를 식별자에서 사용할 수 없으므로 연결된 단어의 첫 글자를 대문자로 변경하여 사용
  - background-color -> backgrounColor

<br />

##### 스타일 식별자 변환

| 스타일시트의 스타일 속성 | 자바스크립트의 스타일 식별자 |
| ------------------------ | ---------------------------- |
| background-image         | backgroundImage              |
| background-color         | backgroundColor              |
| box-sizing               | boxSizing                    |
| list-style               | listStyle                    |

<br />

##### 그라디언트 생성 예제

```html
<!DOCTYPE html>
<html>
<head>
    <title>DOM Basic</title>
    <script>
        // 이벤트 연결
        window.onload = function () {
            // 문서 객체 추가
            var output = '';
            for (var i = 0; i < 256; i++) {
                output += '<div></div>';
            }
            document.body.innerHTML = output;

            // 문서 객체 선택
            var divs = document.querySelectorAll('div');
            for (var i = 0; i < divs.length; i++) {
                // 변수 선언
                var div = divs[i];

                // 스타일 적용
                div.style.height = '2px';
                div.style.background = 'rgb(' + i + ',' + i + ',' + i + ')';
            }
        };
    </script>
</head>
<body>
    
</body>
</html>
```

<br />

### 속성 조작

| 메서드 | 설명 |
| ------ | ---- |
|        |      |

