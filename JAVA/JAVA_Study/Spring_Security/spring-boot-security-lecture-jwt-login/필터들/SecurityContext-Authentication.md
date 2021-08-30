## SecurityContextHolder

(스피링 시큐리티 메뉴얼을 인용한 내용입니다.)

- 스프링 시큐리티 인증 모델의 심장부라고 할 수 있다.
- 간단하게 SecurityContext 를 만드는 방법은 아래와 같다.

  ```java
  SecurityContext context = SecurityContextHolder.createEmptyContext();
  Authentication auth = new TestingAuthenticationToken("username", "password", "ROLE_USER");
  context.setAuthentication(auth);

  SecurityContextHolder.setContext(context);
  ```

- ThreadLocal 안에 SecurityContext 를 담고 두고 있다가 꺼내준다.
- 따라서 request 가 처리되고 나면 SecurityContext 도 clear 된다.
- SecurityContextHolder.MODE_THREADLOCAL 외에도 MODE_GLOBAL이나 MODE_MODE_INHERITABLETHREADLOCAL 같이 정책을 달리하는 옵션을 줄 수 있다. 하지만 매우 특수한 경우일 것이다.

---

## SecurityContext

Authentication 을 담고 있고 전달해주기 위한 문맥객체

---

## Authentication

- Authentication 은 보통 Token 이라는 이름의 클래스로 많이 구현이 된다.
  - UsernamePasswordAuthenticationToken
  - OAuth2AuthenticationToken
  - OAuth2LoginAuthenticationToken
  - OAuth2AuthorizationCodeAuthenticationToken
  - RememberMeAuthenticationToken
  - TestingAuthenticationToken
  - PreAuthenticatedAuthenticationToken
  - RunAsUserToken
  - AnonymouseAuthenticationtoken
- Authentication 인증 이전에는 AuthenticationManager 의 authenticate() 메쏘드의 인자로 인증을 받기위한 input 으로도 쓰이고, 인증이 끝나면 SecurityContext에 담아서 인증된 결과로 사용되는 객체이다.

---

## GrantedAuthority

- UserDetails 객체로 부터 받은 권한 정보를 Authentication 에서도 그대로 사용한다.
- UserDetailsService, UserDetails, Authentication, DecisionManager, DecisionVoter 등 스프링 시큐리티의 곳곳에서 두루 쓰이는 명시적인 권한 객체이다.

## AuthenticationManager

- 인증 여부를 처리해주는 책임져 주는 interface 이다.
- 달랑 하나의 메소드 선언만 있다. 이것만 하겠다는 강한 의지의 인터페이스다. Authentication을 주면 authenticated 여부만 판단해 준다.
  ```java
  Authentication authenticate(Authentication authentication) throws AuthenticationException;
  ```
- 리턴받은 Authentication 을 SecurityContextHolder 에 넣어주는 것은 각각의 Filter 에서 처리해 주어야 한다. (경우에 따라서는 Controller 에서 넣어주기도 한다. 백도어처럼...)

---

## ProviderManager

- ProviderManager 는 AuthenticationProvider 들을 가지고 AuthenticationManager 를 구현한 구현체이다. 즉, AuthenticationManager 그 자체다.
- ProviderManager 는 AuthenticationManager 를 parent로 갖는다. 왜냐하면, ProviderManager 가 가지고 있는 AuthenticationProvider 들이 인증해달라고 요구한 Authentication 객체를 인증해 줄 만한 AuthenticationProvider 를 찾지 못했을 때 parent AuthenticationManager 에게 인증을 위임하기 위해서이다.
- 예를 들어, 웹과 모바일, OAuth2 인증을 모두 사용하는 서버가 있다고 가정해 보자. 모두 AuthenticationProvider 가 별도로 필요한 상황이고 특히나 OAuth2 인증은 여러 사이트의 인증 정보를 처리해 주어야 하기 때문에 복잡한 ProviderManager 관계가 필요하다.

## AuthenticationProvider

- 실질적으로 인증을 제공하는 인터페이스이다. 딱 두개의 메소드를 선언하고 있다.
  ```java
  public interface AuthenticationProvider {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
    boolean supports(Class<?> authentication);
  }
  ```

---

## AbstractAuthenticationProcessingFilter

- UsernamePasswordAuthenticationFilter 와 OAuth2LoginAuthenticationFilter 의 상위 추상 객체이다.
- AuthenticationManager 로부터 Authentication 이 인증되면
  1. SessionAuthenticationStrategy 에 새로운 로그인이 발생했음을 통보한다.
  2. SecurityContext에 Authentication을 담아 SecurityContextHolder에 보관한다.
  3. RememberMeService.loginSuccess 가 호출된다. (RememberMe 가 enable 되어 있다면)
  4. ApplicationEventPublisher 를 통해 InteractiveAuthenticationSuccessEvent 가 발송된다.
  5. AuthenticationSuccessHandler 가 호출된다.
- 인증되지 않으면
  1. SecurityContextHolder 가 clear 된다.
  2. RememberMeService.loginFail 이 호출된다.(RememberMe 가 enable 되어 있다면)
  3. AuthenticationFailerHandler 가 호출된다.

---

## UsernamePasswordAuthenticationFilter

- formLogin() 설정으로 설정한다.
- 실습을 통해 알아본다.

---

## BasicAuthenticationFilter

## Session Management

## RemeberMe

## OpenID

## Anonymouse

## PreAuthentication

## Run-As

## Logout

## Authentication Events
