## Cookie & Session

#### Session Tracking ( 웹 페이지 연결 기능 )

- HTTP 프로토콜은 각 웹 페이지의 상태나 정보를 다른 페이지들과 공유하지 않는 stateless 방식으로 통신
- 웹 페이지나 서블릿끼리 상태나 정보를 공유하려면 웹페이지 연결 기능을 구현해야 함
- 웹 페이지 연동 방법
  - `<hidden>` :
    - HTML의 <hidden> 태그를 이용해 웹 페이지들 사이의 정보를 공유
  - `URL Rewiting` :
    - GET 방식으로 URL 뒤에 정보를 붙여서 다른 페이지로 전송
  - `Cookie` :
    - 클라이언트 PC의 Cookie 파일에 정보를 저장한 후 웹 페이지들이 공유
  - `Session` :
    - 서버 메모리에 정보를 저장한 후 웹 페이지들이 공유

<br/>

#### Cookie

- 정보가 클라이언트 PC에 저장 됨
- 저장 정보 용량에 제한이 있음 (파일 용량은 4kb)
- 보안이 취약
- 클라이언트 브라우저에서 사용 유무를 설정 가능
- 도메인마다 쿠키가 만들어짐
  - 웹 사이트당 하나의 쿠키 생성

<br/>

| 속성                   | Persistence 쿠키                              | Session 쿠키                                  |
| ---------------------- | --------------------------------------------- | --------------------------------------------- |
| 생성 위치              | 파일로 생성                                   | 브라우저 메모리에 생성                        |
| 종료 시기              | 쿠키를 삭제하거나 쿠키 설정 값이 종료 된 경우 | 브라우저를 종료한 경우                        |
| 최초 접속 시 전송 여부 | 최초 접속 시 서버로 전송                      | 최초 접속 시 서버로 전송되지 않음             |
| 용도                   | 로그인 유무 또는 팝업창을 제한                | 사이트 접속 시 Session 인증 정보를 유지 할 때 |

<br/>

#### Session

- 정보가 서버의 메모리에 저장 됨
- 브라우저의 세션 연동은 세션 쿠키를 이용
- 쿠키보다 보안에 유리
- 서버에 부하를 줄 수 있다
- 브라우저(사용자)당 하나의 세션(세션id)이 생성 됨
-  세션은 유효시간이 있음 (기본 30분)
- 로그인 상태 유지 기능이나 쇼핑몰 장바구니 담기 기능 등에 주로 사용