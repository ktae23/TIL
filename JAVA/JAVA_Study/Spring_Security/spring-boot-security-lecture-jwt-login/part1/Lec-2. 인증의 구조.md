# 로그인 하기

## Authentication (인증)의 기본 구조

- 필터들 중에 일부 필터는 인증 정보에 관여한다. 이들 필터가 하는 일은 AuthenticationManager 를 통해 Authentication 을 인증하고 그 결과를 SecurityContextHolder 에 넣어주는 일이다.

  - UsernamePasswordAuthenticationFilter
  - BearerTokenAuthenticationFilter
  - BasicAuthenticationFilter
  - OAuth2LoginAuthenticationFilter

<img src="../images/fig-3-authentication.png" width="600" style="max-width:600px;width:100%;" />

- Authentication 을 제공(Provide) 하는 인증제공자는 여러개가 동시에 존재할 수 있고, 인증 방식에 따라 ProviderManager 도 복수로 존재할 수 있다.
- Authentication 은 인터페이스로 아래와 같은 정보들을 갖고 있다.
  - _Set&lt;GrantedAuthority&gt; authorities_ : 인증된 권한 정보
  - _principal_ : 인증 대상에 관한 정보. 주로 UserDetails 객체가 온다.
  - _credentials_ : 인증 확인을 위한 정보. 주로 비밀번호가 오지만, 인증 후에는 보안을 위해 삭제한다.
  - _details_ : 그 밖에 필요한 정보. IP, 세션정보, 기타 인증요청에서 사용했던 정보들.
  - _boolean authenticated_ : 인증되었는가?

---

## 실습하기

- UsernamePasswordAuthenticationFilter 를 재정의 해서 로그인 테스트 하기
- BasicAuthenticationFilter 테스트
- BearerTokenAuthenticationFilter 테스트 하기
