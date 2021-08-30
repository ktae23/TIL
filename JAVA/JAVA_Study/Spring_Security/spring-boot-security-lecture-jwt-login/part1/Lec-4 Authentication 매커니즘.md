# Authenticatio 메커니즘

## 인증 (Authentication)

<img src="../images/fig-6-Authentication.png" width="600" style="max-width:600px;width:100%;" />

- Authentication 는 인증된 결과만 저장하는 것이 아니고, 인증을 하기 위한 정보와 인증을 받기 위한 정보가 동시에 들어 있다.

  - Credentials : 인증을 받기 위해 필요한 정보, 비번등
  - Principal : 인증된 결과. 인증 대상
  - Details : 기타 정보
  - Authorities : 권한 정보들,

- Authentication 을 구현한 객체들은 일반적으로 Token 이라는 이름의 객체로 구현한다.
- Authentication 객체는 SecurityContextHolder 를 통해 언제든 접근할 수 있다.
-

## 인증 제공자(AuthenticationProvider)

<img src="../images/fig-7-AuthenticationProvider.png" width="600" style="max-width:600px;width:100%;" />

- 인증 제공자(AuthenticationProvider)는 기본적으로 Authentication 을 받아서 인증을 하고 인증된 결과를 다시 Authentication 객체로 전달한다.
- 인증 대상과 방식이 다양할 수 있기 때문에 인증 제공자도 여러개 올 수 있다.

## 인증 관리자(AuthenticationManager)

<img src="../images/fig-8-AuthenticationManager.png" width="600" style="max-width:600px;width:100%;" />

- 인증 제공자들을 관리하는 인터페이스가 AuthenticationManager (인증 관리자)이다. 이 인증 관리자를 구현한 객체가 ProviderManager 이다.
- ProviderManager 도 복수개 존재할 수 있다. OAuth2/OIDC 로그인을 테스트 할 때 확인할 수 있다.
- 개발자가 직접 AuthenticationManager를 정의해서 제공하지 않는다면, AuthenticationManager 를 만드는 AuthenticationManagerFactoryBean 에서 DaoAuthenticationProvider 를 기본 인증제공자로 등록한 AuthenticationManage를 만든다.
- DaoAuthenticationProvider 는 반드시 1개의 UserDetailsService 를 발견할 수 있어야 한다. 만약 없다면 에러이다.
-
