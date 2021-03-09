## JSTL

- JSP Strandard Tag Library
  - JSP 페이지에서 조건문 처리, 반복문 처리를 HTML 태그 형태로 작성할 수 있음

<br>

### JSTL 사용 준비

- http://tomcat.apache.org/download-taglibs.cgi
  - 위의 사이트에서 아래의 3가지 jar파일을 다운로드 한 후 WEB-INF/lib/ 폴더에 복사를 한다.

- Impl : [taglibs-standard-impl-1.2.5.jar](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-impl-1.2.5.jar) ([pgp](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-impl-1.2.5.jar.asc), [sha512](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-impl-1.2.5.jar.sha512))
- Spec : [taglibs-standard-spec-1.2.5.jar](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-spec-1.2.5.jar) ([pgp](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-spec-1.2.5.jar.asc), [sha512](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-spec-1.2.5.jar.sha512))
- EL : [taglibs-standard-jstlel-1.2.5.jar](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-jstlel-1.2.5.jar) ([pgp](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-jstlel-1.2.5.jar.asc), [sha512](https://downloads.apache.org/tomcat/taglibs/taglibs-standard-1.2.5/taglibs-standard-jstlel-1.2.5.jar.sha512))

<br>

##### JSTL이 제공하는 태그 종류

![img](https://cphinf.pstatic.net/mooc/20180130_273/1517290494334HrB7S_PNG/2_6_2_jstl___.PNG)

<br>

### JSTL 사용

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="value1" scope="request" value="kang"></c:set>
<!DOCTYPE html>
...
```

 <br>

#### 코어 태그

![img](https://cphinf.pstatic.net/mooc/20180130_226/1517290578353rKRbE_PNG/2_6_2_jstl_.PNG)

<br>

##### 변수 설정 & 제거

```jsp
<%--변수 설정-->
<c:set var="varName" scope="session" value="someValue" />
<c:set var="varName" scope="request">
some value</c:set>

<%--변수 제거-->
<c:remove vae="varName" scope="request" />
```

<br>

#### 코어태그 : 변수 지원 태그

- 프로퍼티, 맵의 처리

```jsp
<c:set target="${some}" property="propertyName" value="anyValue" />
```

- some 객체가 자바빈일 경우
  - `some.setPropertyName(anyValue)`
- some 객체가 맵일 경우
  - `some.put(propertyName, anyValue);`

- target
  - `<c:set>`으로 지정한 변수 객체

- property
  - 프로퍼티 이름
- value
  - 새로 지정할 프로퍼티 값

<br>

#### 코어 태그 : 흐름 제어 태그 - test

- if문

```jsp
<c:if test="조건">
	test의 조건이 true면
    블럭 안쪽 내용을 처리
</c:if>
```

- if-else문

```jsp
<c:choose>
    <c:when test="조건1">
        조건1이 true면
        블럭 안쪽 내용을 처리
    </c:when>
    <c:choose test="조건2">
        조건2이 true면
        블럭 안쪽 내용을 처리
    </c:when>
    <c:otherwise>
        앞의 <c:when>의 조건들이 모두 만족하지 않을 때 실행 됨
    </c:otherwise>
</c:choose>    
```

<br>

#### 코어 태그 : 흐름 제어 태그 - forEach

- for문

```jsp
<c:forEach var="변수" items="아이템" [begin="시작번호"] [end="끝번호"]>
...
${변수}
...  
</c:forEach>
```

- Var
  - EL에서 사용 될 변수명
- items 
  - items에서 꺼내서 작업을 수행함
  - 배열, List, Iterator, Enumeration, Map 등의 Collection
  - Map이 올 경우 객체는 Map.Enrty로 변수 값을 사용 할때는 ${변수.key}와 ${변수.value}를 사용
- begin
  - items에 지정한 목록에서 값을 읽어 올 인덱스의 시작값
- end
  - items에 지정한 목록에서 값을 읽어 올 인덱스의 끝 값

<br>

#### 코어 태그 : 흐름 제어 태그 - import

- 특정한 URL에 연결하여 결과를 지정한 변수에 저장

```jsp
<c:import url="URL" charEncoding="캐릭터인코딩" var="변수명" scope="범위">
    <c:param name="파라미터 이름" value="파라미터 값" />
</c:import>
```

- URL
  - 결과를 읽어 올 URL
- charEncoding
  - 읽어온 결과를 저장할 때 사용할 캐릭터 인코딩
- var
  - 읽어온 결과를 저장할 변수명
- scope
  - 변수를 저장할 영역
- `<c:param>` 
  - 해당 태그는 url 속성에 지정한 사이트에 연결할 때 전송할 파라미터를 입력한다.
  - URL 사용 시 쿼리 문자열까지 포함하여 사용하고 싶을 때

<br>

#### 코어 태그 : 흐름 제어 태그 - redirect

- 지정한 페이지로 리다이렉트 한다
  - resopnse.sendRedirect()와 비슷

```jsp
<c:redirect url="리다이렉트할 URL">
	<c:prarm name="파라미터 이름" value="파라미터 값"/>
</c:redirect>
```

- url
  - 리다이렉트 URL
- `<c:param>`
  - 해당 태그는 리다이렉트 할 페이지에 전달할 파라미터 이름과 파라미터 값 지정

<br>

#### 코어 태그 : 기타 태그 - out

- JspWriter에 데이터를 출력한다.

```jsp
<c:out value="value" escapeXml="{true|false}" deault="defaultValue" />
```

- Value
  - JspWiter에 출력할 값
  - 일반적으로 String과 같은 문자열 속성
- escapeXml
  - true일 경우 아래 표와 같이 문자를 변경, 생략할 경우 true가 기본값

![image-20210308215516293](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210308215516293.png)

- defaul
  - value 속성에서 지정한 값이 없을 때 사용 될 값 지정