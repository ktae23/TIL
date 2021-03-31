## Log4J DTD 에러 수정

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