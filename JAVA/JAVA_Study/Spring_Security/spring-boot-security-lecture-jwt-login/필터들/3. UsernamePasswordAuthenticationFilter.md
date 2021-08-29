# UsernamePasswordAuthenticationFilter

## 필터가 하는일

- username / password 를 이용한 웹 로그인을 관리함.
- session 을 이용한 사용자 검증
- HttpSecurity 의 formLogin() 에 설정한 정보들이 주로 반영됨.
  ```java
  .formLogin(config->{
    config
            .loginPage("/login")
            .loginProcessingUrl("/login-process")
            .usernameParameter("email")
            .failureUrl("/login-error")
            .defaultSuccessUrl("/", false)
            .authenticationDetailsSource(detailsSource)
            .permitAll()
            ;
  ```
- JWT 토큰을 이용한 로그인이나 OAuth2 로그인도 이 필터를 응용해서 만들 수 있음.
- AuthenticationManager 의 authenticate(Authentication) 메쏘드를 이용해 사용자를 인증함.

---

## 실습1

- 로그인 페이지 구현하기

## 실습2

- Authentication 객체를 새로 디자인 하고 UsernamePasswordAuthenticationFilter 를 확장해 로그인을 구현한다.

## 실습3

- 서로다른 두개의 인증 제공자를 이용해 로그인 하기
  ![fig-3-authentication.png](../images/fig-3-authentication.png)
