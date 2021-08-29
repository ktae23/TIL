# Thymeleaf 기본 기능

## 기본 문법

### 변수 접근

- 기본 : ${...}
- 선택된 객체 안에서 접근 : \*{...}
- 메시지 : #{...}
- URL 링크 : @{...}
- Fragment : ~{...}

### namespace

- xmlns:th="http://www.thymeleaf.org
- xmlns:sec="http://www.thymeleaf.org/extras/spring-security"

### 명령어 (th:\*)

- text : 텍스트 내용
- utext : html
- value : 값
- with : 변수값 지정
- switch, case : switch 문
- if : 조건문
- unless : 부정 조건문
- each : 반복문
