# 폼 로그인

## UsernamePasswordAuthenticationFilter

- form 인증을 처리해주는 필터로 스프링 시큐리티에서 가장 일반적으로 쓰인다.
- 주요 설정 정보

  - filterProcessingUrl : 로그인을 처리해 줄 URL (POST)
  - username parameter : POST에 username에 대한 값을 넘겨줄 인자의 이름
  - password parameter : POST에 password에 대한 값을 넘겨줄 인자의 이름
  - 로그인 성공시 처리 방법
    - defaultSuccessUrl : alwaysUse 옵션 설정이 중요
    - successHandler
  - 로그인 실패시 처리 방법
    - failureUrl
    - failureHandler
  - authenticationDetailSource : Authentication 객체의 details 에 들어갈 정보를 직접 만들어 줌.

## LogoutFilter

- 로그아웃을 지원
- POST로 요청하는 것이 원칙
- ## 주요 설정 정보

## 실습

### login-basic

### login-custom-page

### login-custom-filter

### login-multi-filter

### login-multi-provider

### UsernamePasswordAuthenticationFilter 확장해서 구현하기

- 확장해서 구현하기

### AuthenticaitonDetailSource 를 확장해서 구현하기

-
