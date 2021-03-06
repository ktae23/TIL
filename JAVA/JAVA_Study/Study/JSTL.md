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

#### 코어 태그 : 흐름 제어 태그

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

