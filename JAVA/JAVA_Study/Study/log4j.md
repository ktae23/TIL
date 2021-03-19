## 27.8 log4j란?

- 애플리케이션에서는 웹 사이트에 접속한 사용자 정보나 각 클래스의 메서드 호출 시각 등 여러가지 정보를 파일로 저장해서 관리 함
- 로그 관련 기능을 제공하는 것이 log4j로 독립적으로 라이브러리를 설치해서 사용 할 수 있고 메이븐과 같은 빌드 툴에서는 자동으로 설치 됨
- 기능 관련 설정은 log4j.xml 파일에서 수행
  - 한번이라도 열면 에러 표시 남
    - 이클립스 버그
- 콘솔에 검은 글씨로 나오는 것이 스프링에서 찍어주는 로그

<br/>

##### 기존 1~3라인 코드

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration PUBLIC "-//APACHE//DTD LOG4J 1.2//EN" "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">

```

<br/>

##### 수정 1~3라인 코드

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration PUBLIC "-//APACHE//DTD LOG4J 1.2//EN" "http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">

```

<br/>

- local DTD를 URL로 변경해줌
- 수정 이후 에러 나오지 않음

<br/>

#### 태그

- `<Appender>`
  - 로그를 콘솔로 출력할지 파일로 출력할지 출력 위치 설정
- `<Layout>`
  - 어떤 형식으로 출력할지 설정

- `<Logger>`
  - 로그 레벨을 설정하여 선택적으로 로그 출력 가능

<br/>

#### Appender 태그 클래스

- ConsoleAppender
  - 콘솔에 로그 메시지 출력
- FileAppender
  - 파일에 로그 메시지 출력

- RollingFileAppender
  - 파일 크기가 일정 기준을 넘으면 기존 파일을 백업 파일로 바꾸고 처음부터 다시 기록
- DailyRollingAppender
  - 설정한 기간 단위로 새 파일을 만들어 로그 메시지 기록

<br/>

#### 출력 형식

- `%p`
  - debug, info, error, fatal 등 로그 레벨 이름 출력

- `%m`
  - 로그 메시지 출력
- `%d`
  - 로깅 이벤트 발생 시각 출력
- `%F`
  - 로깅이 발생한 프로그램 파일 이름 출력
- `%l`
  - 로깅이 발생한 caller의 정보 출력
- `%L`
  - 로깅이 발생한 caller의 라인 수 출력
- `%M`
  - 로깅이 발생한 메서드 이름 출력
- `%c`
  - 로깅 메시지 앞에 전체 패키지 이름이나 전체 파일 이름 출력
- 등등

<br/>

#### 레벨

- FATAL
  - 시스템 차원에서 심각한 문제가 발생해 애플리케이션 작동이 불가할 경우에 해당하는 레벨
  - 일반적으로 애플리케이션에서는 사용할 일이 없음
- ERROR
  - 실행  중 문제가 발생한 상태
- WARN
  - 향후 시스템 오류의 원인이 될 수 있는 경고 메시지
- INFO
  - 로그인, 상태 변경과 같은 실제 애플리케이션 운영과 관련된 정보 메시지 나타냄
- DEDBUG
  - 개발 시 디버깅 용도로 사용한 메시지 나타냄
  - 배포시는 사용하지 않음
- TRACE
  - DEBUG 레벨보다 상세한 로깅 정보를 출력하기 위해 도입 됨

<br/>

교재 980p ~ 985p 참조 