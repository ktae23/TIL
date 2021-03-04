## Scope

![네이버 부스트코스 백엔드 과정](https://cphinf.pstatic.net/mooc/20180129_297/1517205425406SvaC6_JPEG/2_5_1_scope_.jpg)

### Page scope (Least Visivle)

- 선언된 한 페이지 내에서만 사용 가능
- 페이지 내에서 지역변수처럼 사용

<br/>

- PageContext 추상 클래스를 사용
  - JSP페이지에서 pageContext 내장 객체로 사용 가능
  - pageContext객체.setAttribute() or .getAttribute()
  - 사용 방법은 나머지 스코프들 과 같음
- forward가 될 경우 해당 page scope에 지정된 변수는 사용 불가
  - 지역변수처럼 사용 되는 것이 다른 스코프들과 다름
- EL표기법 또는 JSTL로 사용 할 때 용이



<br/>

### Request scope

- 클라이언트의 하나의 요청에 대해 서버가 요청을 처리하고 응답을 보내는 동안 사용 가능

<br/>

- http요청을 WAS가 받아서 웹 브라우저에게 응답할 때까지 변수가 유지되는 경우 사용
- HttpServletRequest 내장 객체를 사용
  - 서블릿에서는 HttpServletRequest 내장 객체를 사용
  - JSP에서는 request 내장 변수 사용
- forward시 값을 유지하고자 사용
  - 항상 forward 전에 request 객체의 setAttribute()로 값을 설정 한 뒤 서블릿이나 JSP에서 결과 값을 getAttribute()로 획득
  - foward 동안 유지 되도록 하는 스코프

<br/>

### Session scope

- Session 객체가 생성 되어 소멸 될 때까지 사용 가능
- 여러개 요청에도 객체가 남아 있는 한 사용 가능
- 상태 유지할 때 사용

<br/>

- 웹 브라우저별로 변수가 관리되는 경우 사용

- 웹 브라우저의 탭 사이에는 세션 정보가 공유 됨
  - 각각의 탭에서 같은 세션 정보 사용 가능
- HttpSession 인터페이스를 구현한 객체를 사용
  - 서블릿에서는 HttpServletRequest의 getSession()메서드 이용하여 session 객체 얻음
  - JSP에서는 session 내장 변수 사용
- 값을 저장 할때는 session객체.setAttribute()
- 값을 읽을 때는 session객체.getAttribute()
- `장바구니 기능`처럼 사용자별로 유지 되어야 할 정보에 사용
  - 시간 제한 또는 브라우저 종료 등에 따라 사용 종료

<br/>

### Application scope (Most Visible)

- 하나의 어플리케이션이 생성 되어 소멸 될 때까지 유지 되어 사용 가능

<br/>

- 웹 어플리케이션이 시작되고 종료될 때까지 변수가 유지되는 경우 사용
- ServletContext 인터페이스를 구현한 객체를 사용
  - 서블릿에서는 getServletContext() 메서드를 사용해 application 객체 이용
  - JSP에서는 application 내장 객체 이용
- 웹 어플리케이션 하나 당 하나의 application 객체가 사용 됨
- 값을 저장 할때는 application객체.setAttribute()
- 값을 읽을 때는 application객체.getAttribute()
- 모든 클라이언트가 공통으로 사용해야할 값 저장하여 사용

<br/>

##### * getAttribute()의 반환 값은 Object로 나오기 때문에 다운캐스팅 필수