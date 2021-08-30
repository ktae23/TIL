## 5.2.1 CSRF (Cross Site Request Forgery)

예를 들어 한 사용자가 은행 사이트에 접속해 송금을 한다고 생각해 보자. 송금한 form은 일반적으로 아래와 같을 것이다.

```
<form method="post" action="/송금">
  <input type="text" name="금액" value="100000"/>
  <input type="text" name="받는계좌"  value="3232"/>
  <input type="text" name="보내는계좌"  value="기본계좌"/>
  <input type="submit" name="송금" />
</form>
```

그러면 HTTP 요청이 아래와 같이 전송된다.

```
POST /송금 HTTP/1.1
HOST: 은행.사이트.com
Cookie: JSESSIONID=랜덤
Content-Type: application/x-www-form-urlencoded

금액=100000&받는계좌=3232&보내는계좌=기본계좌
```

이 사용자가 은행사이트를 로그아웃하지 않은 상태에서 악성 사이트에 접속을 했다고 가정해 보자. 이 사이트에서는 로또 당첨! 이란 버튼을 만들어 놓고 기다린다.

```
<form method="post" action="https://은행.사이트.com/송금">
  <input type="text" name="금액" value="100000"/>
  <input type="text" name="받는계좌"  value="악성계좌"/>
  <input type="text" name="보내는계좌"  value="기본계좌"/>
  <input type="submit" name="로또 당첨!" />
</form>
```

이 사용자가 로또 당첨! 이란 버튼을 누르는 순간 10만원이 기본 계좌에서 빠져나간다. 악성 사이트의 서버는 브라우저의 쿠키값을 알아낼 수는 없지만 은행 사이트와 연결된 쿠키가 브라우저에 묻어있기 때문에 은행 사이트는 요청을 그대로 처리할 수 밖에 없다. 더구나 이런 과정을 스크립트로 자동화할 수도 있다. 페이지를 여는 순간 자동실행하게 할 수 있다.
구지 악성사이트가 아니라 잘 알려진 사이트라도 해커가 사이트를 해킹해 해당 스크립트를 삽입해 넣어놓을 수 있다.

---

## CSRF 공격으로 부터 방어하기

CSRF 공격이 가능했던 이유는 은행 사이트의 요청과 악성사이트의 요청이 완전히 동일하기 때문이다. 이 문제를 해결하려면 은행사이트의 요청을 악성사이트가 완전히 똑같은 모양으로 복사할 수 없게 만들어야 한다.

이를 위해 스프링에서는 두가지 방법을 제공한다.

- \_csrf 동기화 토큰 발행 방식
- 세션 쿠키에 samesite 속성을 주는 방법
