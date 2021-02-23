## HTML 입력 양식 태그

<br />

### 입력 양식 

- form 태그로 영역을 설정하고, 내부에 input 태그를 넣어 만든다  

<br />

```html
<form action="전송 위치" method="전송 방식">

</form>
```

<br />

- action = 데이터를 전달할 목적지
- method 
  - GET : 주소에 데이터를 직접 입력해 전달
    - 주소에 데이터를 입력해서 전달
    - 주소값이 변경 됨
    - 크기 한정
  - POST : 별도의 방법을 사용해 데이터를 해당 주소로 전달
    - 주소가 변경 되지 않음
    - 별도로 전송하므로 데이터 용량 제한 없음

<br />

#### 입력 연습

```html
<!DOCTYPE html>
<html>
<head>
    <title>입력 연습</title>
</head>
<body>
    <form>
        <!-- 입력 받기 -->
        <input type="text" name="text" value="text" /><br />
        <input type="password" name="password" value="password" /><br />
        <input type="file" name="file" value="file" /><br />
        <input type="checkbox" name="checkbox" value="checkbox" /><br />
        <input type="radio" name="radio" value="radio" /><br />

        <!-- 숨김 -->
        <input type="hidden" name="hidden" value="hidden" /><br />

        <!-- 버튼 -->
        <input type="button" value="button" /><br />
        <input type="reset" value="reset" /><br />
        <input type="submit" value="submit" /><br />
        <input type="image" src="http://placehold.it/100x100" />
        
        <!-- 라벨 -->
        <label for="name">이름</label><!-- for 속성에 input 태그의 id를 입력하여 label 태그를 클릭했을 때 input 태그에 자동으로 포커스 가도록 설정 가능 -->
        <input id="name" type="text">
        
    </form>
</body>
</html>

```

<br />

##### 선택 입력 연습

```html
<!DOCTYPE html>
<html>
<head>
    <title>선택 입력 연습</title>
</head>
<body>
    
	<!--multiple 속성 제외하면 1개만 선택-->
    <select multiple="multiple"> 
        <option>김밥</option>
        <option>떡볶이</option>
        <option>순대</option>
        <option>어묵</option>
    </select>
    
    <!--여러 대상 중 하나만 선택하도록 name 속성 부여-->
     <table>
            <tr>
                <td><label for="username">이름</label></td>
                <td><input id="username" type="text" name="username" /></td>
            </tr>
            <tr>
                <td>성별</td>
                <td>
                    <input id="man" type="radio" name="gender" value="m" />
                    <label for="man">남자</label>
                    <input id="woman" type="radio" name="gender" value="w" />
                    <label for="woman">여자</label>
                </td>
            </tr>
        </table>  
        <input type="submit" value="가입" />
    
    <!--선택지 그룹 묶기-->
    <select>
        <optgroup label="HTML5">
            <option>Multimedia Tag</option>
            <option>Connectivity</option>
            <option>Device Access</option>
        </optgroup>
        <optgroup label="CSS3">
            <option>Animation</option>
            <option>3D Transform</option>
        </optgroup>
    </select>
    
     <!--연관 있는 양식끼리 그룹 묶기-->
    <fieldset>
            <legend>입력 양식</legend>
            <table>
                <tr>
                    <td><label for="name">이름</label></td>
                    <td><input id="name" type="text" /></td>
                </tr>
                <tr>
                    <td><label for="mail">이메일</label></td>
                    <td><input id="mail" type="email" /></td>
                </tr>
            </table>
            <input type="submit" />
        </fieldset>
    
    <!--들여쓰기 주의사항-->
    <textarea>
    	들여쓰기 적용 됨
        들여쓰기 적용 됨
    </textarea>
    <textarea>들여쓰기 적용 안됨
        들여쓰기 적용 안됨</textarea>
    
</body>
</html>

```



| 태그                   | 속성        | 설명                                                         |
| ---------------------- | ----------- | ------------------------------------------------------------ |
| form                   | 보이지 않음 | 입력 양식의 시작과 끝 표시                                   |
| input                  | text        | 글자 입력 양식 생성                                          |
|                        | button      | 버튼 생성                                                    |
|                        | checkbox    | 체크 박스 생성                                               |
|                        | file        | 파일 입력 양식 생성                                          |
|                        | hidden      | 해당 내용 숨김                                               |
|                        | image       | 이미지 형태 생성                                             |
|                        | password    | 비밀번호 입력 양식 생성                                      |
|                        | radio       | 라디오 버튼 생성                                             |
|                        | reset       | 초기화 버튼 생성                                             |
|                        | submit      | 제출 버튼 생성                                               |
| textarea               | cols/rows   | 여러 행의 글자 입력 양식 생성<br />cols는 너비를 지정<br />row는 높이를 지정 |
| select optgroup option |             | 선택 양식 생성<br />옵션 그룹화<br />옵션 생성               |
| fieldset legend        |             | 입력 양식의 그룹 지정<br />입력 양식 그룹의 이름 지정        |

<br />

## HTML 문서 구조화

### 공간 분할 태그

- div
  - 블럭 형식으로 한 블럭의 영역을 구분함
  - h1~6, p, 목록, 테이블 태그도 블럭 형식
- span
  - 인라인 형식으로 차지한 글자 크기만큼 영역을 구분 함
  - a, input, 글자, 입력 양식 태그도 인라인 형식

<br />

### 시맨틱 태그

- semantic(의미론적인) 태그는 구분 한 공간에 의미를 부여함

<br />

![HTML5 Structure Doc](https://cloud.netlifyusercontent.com/assets/344dbf88-fdf9-42bb-adb4-46f01eedd629/b5aae2c5-9c90-49ca-8a93-44dd63cf132e/html5-structure.png)

<br />

| 태그    | 설명                              |
| ------- | --------------------------------- |
| header  | 머리말(페이지 제목, 페이지 소개)  |
| nav     | 하이퍼링크들을 모아 둔 내비게이션 |
| aside   | 본문 흐름에 벗어나는 노트나 팁    |
| section | 문서의 장이나 절에 해당하는 내용  |
| article | 본문과 독립적인 콘텐츠 영역       |
| footer  | 꼬리말(저자나 저작권 정보)        |

