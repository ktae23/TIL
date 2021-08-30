# 스프링 부트 시큐리티 강의 (옥탑방개발자)

스프링 부트 시큐리티에 대한 강의 자료를 정리하였습니다.

## 사이트 환경

인터넷에 사이트를 개발하고 서비스를 하게 되면, 서비스 안에는 서비스를 사용하는 User 들의 리소스(정보)들이 위치하게 된다. 관리자는 이들 리소스들을 위임받아 관리하는 것이기 때문에, 악의적인 사용자들로부터 리소스를 잘 보호해야 하고, 효과적으로 관리해야 한다.

<img src="./images/fig-0-site-securities.png" width="600" style="max-width:600px;width:100%;" />

스프링 부트의 security 는 이들 리소스를 잘 보호할 수 있는 기본 메커니즘과 라이브러리를 제공한다.
이 강의는 이들 라이브러리를 어떻게 이해햐야 하는지, 그리고 어떻게 잘 사용할 수 있는지에 대한 내용을 담은 것이다.

## 선수 지식

이 강의는 Spring Security를 실전에서 능숙하게 다루는 것을 목표로 합니다. 그렇기 때문에 아래와 같은 선수지식들을 필요로 합니다. 혹시 이해가 부족하거나 잘 모르는 부분이 있다면 미리 공부를 하고 이 강의를 들으실 것을 권장드립니다.

- Java : jdk 11 이상 (moden java 에 대해 알아야 함)
- spring boot : 스프링 애플리케이션 프레임워크
- gradle : 프로젝트 관리 및 빌드
- JUnit5(Jupyter)와 spring test : 기본적인 기능 테스트를 위해 필요함.
- spring mvc 와 RESTFul 서비스 : 웹 MVC
- spring data jpa : 인증을 DB에 저장함.
- lombok : 실전 프로그래밍에 도움이 많이 줌.
- thymeleaf : 웹 프로그램을 지원함
- mysql : 데이터 테스트
- IntelliJ IDE

## 이 강의에서 다룰 내용

- Gradle 멀티 프로젝트 구성과 모듈 프로젝트 개발
- Spring Security의 기본 구조
- Spring Security 를 활용한 로그인 방법 (Authentication)
- Spring Security 를 활용한 권한 체크 방법 (Authorization)
- Ajax 와 OAuth2 인증 : ajax / OAuth2 인증 방식

스프링 인증(authentication)과 인가라는 말을 많이 쓰는데, 인가라는 말보다는 권한(authorization) 이라는 용어로 얘기를 하도록 하겠다. 권한(authorization)은 역할(role)을 좀 더 세분화한 개념으로 이해하면 된다. 초기의 웹 security 가 만들어질 때(acegi security)는 role이 권한의 역할을 했지만, 이 개념이 역할이라는 개념과 혼용되어 쓰이기 때문에 많은 오해들이 발생했다. 그래서 role 보다 좀 더 구체화된 권한의 실체로 authorization 이라는 용어가 도입되고 실제적인 권한의 개념으로 발전했다.

## 인증 (로그인)

- 인증 방식에 따라
  - 폼 인증
  - JWT 인증
  - oauth2 인증
- 서버의 세션 상태에 따라

  - sessionful
  - sessionless

- 데이터 관리
  - In Memory
  - JPA
  - Mongo Data
  - LDAP
  - etc ...

## 권한 관리

## 실전 프로젝트
