# 요청 파이프라인 전체 추적

`webClient.get().uri("/x").retrieve().bodyToMono(Foo.class)` 한 줄이 실제로 거치는 모든 홉을 v6.2.19 소스로 끝까지 따라간다. 핵심 결론은 **이 체인 전체가 조립만 할 뿐 아무 I/O도 일으키지 않는다**는 것이다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

추적 대상 코드:

```java
Mono<Foo> mono = webClient.get()
        .uri("/foo/{id}", 42)
        .retrieve()
        .bodyToMono(Foo.class);
```

이 네 줄이 거치는 홉은 다음과 같다.

```
1.  DefaultWebClient.get()                       → methodInternal(HttpMethod.GET)
2.  methodInternal()                             → new DefaultRequestBodyUriSpec + defaultRequest 적용
3.  DefaultRequestBodyUriSpec.uri(tmpl, vars)    → uriBuilderFactory + URI_TEMPLATE_ATTRIBUTE 기록
4.  DefaultRequestBodyUriSpec.retrieve()         → exchange() 호출 + new DefaultResponseSpec
5.  DefaultRequestBodyUriSpec.exchange()         → initRequestBuilder() + Mono.deferContextual(...)
    ────────────────── 여기까지 assembly time (I/O 없음) ──────────────────
6.  [구독] deferContextual 람다 실행             → Observation 시작, 필터 체인 합성, ClientRequest 빌드
7.  finalFilterFunction.apply(exchangeFunction)  → 필터 + exchange를 하나의 ExchangeFunction으로 접기
8.  ExchangeFunctions.DefaultExchangeFunction.exchange(request)
9.  connector.connect(method, url, req -> clientRequest.writeTo(req, strategies))
10. new DefaultClientResponse(httpResponse, ...)
11. DefaultResponseSpec.bodyToMono(Foo.class)    → responseMono.flatMap(handleBodyMono)
12. handleBodyMono()                             → applyStatusHandlers() → 바디 디코딩
```

```mermaid
sequenceDiagram
    autonumber
    participant App as 애플리케이션
    participant WC as DefaultWebClient
    participant Spec as DefaultRequestBodyUriSpec
    participant RS as DefaultResponseSpec
    participant FF as ExchangeFilterFunction 체인
    participant EF as DefaultExchangeFunction
    participant CN as ClientHttpConnector
    participant CR as ClientRequest

    rect rgb(245, 245, 250)
    note over App,CR: assembly time — 객체 조립만, I/O 없음
    App->>WC: get()
    WC->>Spec: methodInternal(GET)<br/>defaultRequest.accept(spec)
    Spec-->>App: RequestHeadersUriSpec
    App->>Spec: uri("/foo/{id}", 42)
    Spec->>Spec: uriBuilderFactory.uriString(tmpl)<br/>attribute(URI_TEMPLATE_ATTRIBUTE, …)
    App->>Spec: retrieve()
    Spec->>Spec: exchange() → initRequestBuilder()
    Spec->>Spec: Mono.deferContextual(λ) 생성
    Spec->>RS: new DefaultResponseSpec(method, uri, responseMono, defaultStatusHandlers)
    App->>RS: bodyToMono(Foo.class)
    RS-->>App: Mono&lt;Foo&gt; (아직 아무것도 안 함)
    end

    rect rgb(250, 245, 240)
    note over App,CR: subscribe time — 여기서야 실행
    App->>Spec: subscribe()
    Spec->>Spec: Observation.start()
    Spec->>Spec: filterFunctions.andThen(ObservationFilterFunction)
    Spec->>CR: requestBuilder.build() → 불변 BodyInserterRequest
    Spec->>FF: finalFilterFunction.apply(exchangeFunction).exchange(request)
    FF->>EF: exchange(request)
    EF->>CN: connect(method, url, req -> clientRequest.writeTo(req, strategies))
    CN->>CR: writeTo(nativeRequest, strategies)
    CR-->>CN: Mono&lt;Void&gt; (쓰기 완료)
    CN-->>EF: Mono&lt;ClientHttpResponse&gt;
    EF->>EF: new DefaultClientResponse(httpResponse, strategies, …)
    EF-->>FF: Mono&lt;ClientResponse&gt;
    FF-->>RS: Mono&lt;ClientResponse&gt;
    RS->>RS: handleBodyMono(response, response.bodyToMono(Foo.class))
    RS->>RS: applyStatusHandlers(response)
    RS-->>App: Foo 또는 WebClientResponseException
    end
```

---

## 2. 왜 알아야 하는가 (Why)

이 파이프라인을 모르면 세 가지 버그를 반복한다.

**(1) "요청이 안 나간다"**
`Mono`를 만들어 놓고 구독하지 않으면 **단 한 바이트도 나가지 않는다**. `exchange()`의 본체가 `Mono.deferContextual` 안에 들어 있기 때문이다(`DefaultWebClient.java:452`). 반대로 **같은 `Mono`를 두 번 구독하면 요청이 두 번 나간다**. `defer`가 구독마다 람다를 다시 실행하기 때문이다.

**(2) "헤더를 필터에서 바꿨는데 반영이 안 된다"**
`ClientRequest`는 불변이다(`DefaultClientRequestBuilder.java:179-212`). 필터에서 `request.headers().add(...)`를 호출하면 `UnsupportedOperationException`이 난다 — `HttpHeaders.readOnlyHttpHeaders`로 감싸져 있다(`DefaultClientRequestBuilder.java:204`). 반드시 `ClientRequest.from(request)`로 새 객체를 만들어야 한다.

**(3) "타임아웃 예외 타입이 뭔지 모르겠다"**
I/O 단계의 모든 실패는 `WebClientRequestException`으로 래핑된다(`ExchangeFunctions.java:106`, `ExchangeFunctions.java:138-140`). HTTP 상태 오류인 `WebClientResponseException`과 **완전히 다른 가지**다. 재시도 전략을 짤 때 이 구분이 전부다.

---

## 3. 내부 구현 분석 (How)

### 3.1 홉 1-2: get() → methodInternal()

```java
// DefaultWebClient.java:139-142
@Override
public RequestHeadersUriSpec<?> get() {
    return methodInternal(HttpMethod.GET);
}

// DefaultWebClient.java:179-185
private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
    DefaultRequestBodyUriSpec spec = new DefaultRequestBodyUriSpec(httpMethod);
    if (this.defaultRequest != null) {
        this.defaultRequest.accept(spec);
    }
    return spec;
}
```

모든 HTTP 메서드 진입점이 `methodInternal()` 하나로 수렴한다. `get()`의 반환 타입이 `RequestHeadersUriSpec<?>`인데 실제 객체는 `DefaultRequestBodyUriSpec`이다 — **런타임 객체는 바디를 받을 수 있지만 타입이 그 메서드를 숨긴다.**

주목할 점: `defaultRequest` consumer가 **spec 생성 직후, uri() 호출 전에** 실행된다(`DefaultWebClient.java:181-183`). 즉 `Builder.defaultRequest()`에 등록한 설정은 가장 먼저 적용되고, 이후 요청별 호출이 덮어쓸 수 있다.

### 3.2 홉 3: uri() — 템플릿과 attribute

```java
// DefaultWebClient.java:229-234
@Override
public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
    UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
    attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
    return uri(uriBuilder.build(uriVariables));
}
```

세 가지가 일어난다.

1. `uriBuilderFactory`(빌더가 `baseUrl`로 만든 `DefaultUriBuilderFactory`)가 템플릿을 받아 `UriBuilder`를 만든다.
2. **확장 전 템플릿 문자열**을 `URI_TEMPLATE_ATTRIBUTE`에 저장한다. 이 키는 `WebClient.class.getName() + ".uriTemplate"`이다(`DefaultWebClient.java:75`).
3. 변수를 확장해 `URI`를 만들고 `this.uri`에 담는다(`DefaultWebClient.java:256-259`).

(2)가 왜 중요한가 — **메트릭 카디널리티**다. `/foo/42`, `/foo/43`을 각각 태그로 쓰면 메트릭이 폭발한다. Observation 컨텍스트가 이 attribute를 꺼내 `uriTemplate`으로 쓴다(`DefaultWebClient.java:464`).

```java
observationContext.setUriTemplate((String) request.attribute(URI_TEMPLATE_ATTRIBUTE).orElse(null));
```

`uri(URI)` 오버로드를 쓰면 이 attribute가 **설정되지 않는다**(`DefaultWebClient.java:256-259`에 `attribute()` 호출이 없다). 직접 `URI`를 만들어 넘기면 메트릭에서 URI 템플릿을 잃는다. 자세한 내용은 [10-observability.md](10-observability.md).

`uri(Function<UriBuilder, URI>)` 단일 인자 오버로드도 attribute를 설정하지 않는다(`DefaultWebClient.java:250-253`). 반면 `uri(String, Function)` 오버로드는 설정한다(`DefaultWebClient.java:243-248`).

### 3.3 홉 4: retrieve() — exchange()를 즉시 호출한다

```java
// DefaultWebClient.java:413-417
@Override
public ResponseSpec retrieve() {
    return new DefaultResponseSpec(
            this.httpMethod, initUri(), exchange(), DefaultWebClient.this.defaultStatusHandlers);
}
```

`retrieve()`는 **`exchange()`를 그 자리에서 호출한다.** 하지만 `exchange()`가 돌려주는 것은 `Mono<ClientResponse>`이고, 그 `Mono`는 구독 전까지 아무 일도 하지 않는다. `DefaultResponseSpec`은 그 `Mono`를 `responseMono` 필드에 보관한다(`DefaultWebClient.java:540`, `DefaultWebClient.java:551`).

`initUri()`는 `uri()`를 호출하지 않았을 때의 폴백을 담당한다(`DefaultWebClient.java:506-508`).

```java
private URI initUri() {
    return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
}
```

즉 `uri()`를 생략하면 **baseUrl 자체**로 요청이 간다.

### 3.4 홉 5: exchange() — assembly time과 subscribe time의 경계

이 메서드가 문서 전체의 핵심이다(`DefaultWebClient.java:449-490`).

먼저 구조를 정확히 짚는다. **v6.2.19에서 이 `exchange()`는 내부 helper가 아니라 `WebClient.RequestHeadersSpec.exchange()`의 public 구현이다.** 그 인터페이스 메서드가 `@Deprecated`(since 5.3, `WebClient.java:644-645`)이기 때문에 구현에도 `@SuppressWarnings("deprecation")`이 붙어 있다(`DefaultWebClient.java:448`).

따라서 v6.2.19의 홉 구조는 이렇게 읽어야 한다.

```
retrieve()         ─┐
exchangeToMono()   ─┼─▶ 모두 같은 public deprecated exchange() 로 수렴한다
exchangeToFlux()   ─┤
직접 호출한 exchange() ─┘
```

**네 종단 API가 전부 이 메서드 하나를 거친다.** 차이는 그 위에 무엇을 덮느냐뿐이다 — `retrieve()`는 `DefaultResponseSpec`을, `exchangeTo*()`는 `releaseIfNotConsumed` 래핑을 덮고, 직접 호출하면 아무것도 덮이지 않는다(그래서 누수가 난다). 이 구조가 [03-retrieve-vs-exchange.md](03-retrieve-vs-exchange.md)의 출발점이다.

> 7.0에서는 public `exchange()`가 제거되고 같은 이름의 private helper만 남는다. 즉 **본문은 거의 그대로인데 공개 여부가 달라진다** — 파이프라인 추적 자체는 유효하지만 홉 이름을 "deprecated 종단 API"로 읽을 수는 없게 된다. 차이는 [12-boot4-spring7-migration.md](12-boot4-spring7-migration.md).

```java
@SuppressWarnings("deprecation")
@Override
public Mono<ClientResponse> exchange() {
    ClientRequest.Builder requestBuilder = initRequestBuilder();      // ← ① assembly time
    return Mono.deferContextual(contextView -> {                      // ← ② 여기부터 subscribe time
        Observation observation = ClientHttpObservationDocumentation.HTTP_REACTIVE_CLIENT_EXCHANGES.observation(
                observationConvention, DEFAULT_OBSERVATION_CONVENTION,
                () -> new ClientRequestObservationContext(requestBuilder), observationRegistry);
        observation
                .parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null))
                .start();
        ExchangeFilterFunction filterFunction = new ObservationFilterFunction(observation.getContext());
        if (filterFunctions != null) {
            filterFunction = filterFunctions.andThen(filterFunction);  // ← ③ 사용자 필터가 앞
        }
        ClientRequest request = requestBuilder.build();                // ← ④ 불변 객체 확정
        if (observation.getContext() instanceof ClientRequestObservationContext observationContext) {
            observationContext.setUriTemplate((String) request.attribute(URI_TEMPLATE_ATTRIBUTE).orElse(null));
            observationContext.setRequest(request);
        }
        final ExchangeFilterFunction finalFilterFunction = filterFunction;
        Mono<ClientResponse> responseMono = Mono.defer(
                        () -> finalFilterFunction.apply(exchangeFunction).exchange(request))  // ← ⑤
                .checkpoint("Request to " +
                        WebClientUtils.getRequestDescription(request.method(), request.url()) +
                        " [DefaultWebClient]")
                .switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR);         // ← ⑥
        if (this.contextModifier != null) {
            responseMono = responseMono.contextWrite(this.contextModifier);
        }
        final AtomicBoolean responseReceived = new AtomicBoolean();
        return responseMono
                .doOnNext(response -> responseReceived.set(true))
                .doOnError(observation::error)
                .doFinally(signalType -> {
                    if (signalType == SignalType.CANCEL && !responseReceived.get() &&
                            observation.getContext() instanceof ClientRequestObservationContext observationContext) {
                        observationContext.setAborted(true);           // ← ⑦
                    }
                    observation.stop();
                })
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
    });
}
```

번호별로:

**① 유일한 assembly-time 작업.** `initRequestBuilder()` (`DefaultWebClient.java:492-504`)는 `deferContextual` **바깥**에서 호출된다. `ClientRequest.create(httpMethod, initUri())`에 헤더·쿠키·attribute를 모으고, `httpRequestConsumer`와 바디 `inserter`가 있으면 붙인다. 다만 이건 가변 빌더일 뿐이고 요청이 확정된 것은 아니다.

> 주의: `initHeaders()`는 `out.putAll(defaultHeaders)` 후 `out.putAll(this.headers)`를 한다(`DefaultWebClient.java:510-517`). `putAll`은 **키 단위 교체**이므로, 같은 헤더 이름을 요청별로 지정하면 기본 헤더 값이 누적되지 않고 **대체**된다.

**② `Mono.deferContextual`** — 구독할 때마다 람다가 다시 실행된다. 따라서 `Observation` 생성, 필터 합성, `ClientRequest` 확정은 **모두 구독 시점**이고, **구독마다 새로** 일어난다. `retry()`가 올바른 새 요청을 만들어내는 이유가 여기 있다.

**③ 필터 합성 순서** — `filterFunctions.andThen(observationFilter)`다. `andThen`의 의미상 **사용자 필터가 먼저(바깥), `ObservationFilterFunction`이 나중(안쪽)** 실행된다. `ObservationFilterFunction`은 `next.exchange(request)`의 결과에 `doOnNext(clientContext::setResponse)`를 붙인다(`DefaultWebClient.java:745-752`). 즉 **관측 컨텍스트에 기록되는 응답은 사용자 필터가 가공하기 전의 원본**이다.

**④ `requestBuilder.build()`** — `DefaultClientRequestBuilder.build()`가 `BodyInserterRequest`를 만든다(`DefaultClientRequestBuilder.java:172-176`). 생성자에서 불변화가 일어난다(`DefaultClientRequestBuilder.java:198-212`).

```java
this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
this.cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);
this.attributes = Collections.unmodifiableMap(attributes);

Object id = attributes.computeIfAbsent(LOG_ID_ATTRIBUTE, name -> ObjectUtils.getIdentityHexString(this));
this.logPrefix = "[" + id + "] ";
```

`LOG_ID_ATTRIBUTE`가 여기서 생성된다 — 로그 상관관계의 기반이다(`ClientRequest.java:56`).

**⑤ 중첩된 `Mono.defer`** — `deferContextual` 안에 또 `defer`가 있다. 이유는 `finalFilterFunction.apply(exchangeFunction)`이 **호출 시점에 새 `ExchangeFunction`을 합성**하기 때문이다. 그 합성 자체도 구독 시점으로 미룬다.

**⑥ `switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR)`와 `contextModifier` 배선** — 하위 HTTP 클라이언트가 응답 없이 완료하는 비정상 상황을 명시적 예외로 바꾼다(`DefaultWebClient.java:77-78`).

바로 뒤의 `if (this.contextModifier != null) { responseMono = responseMono.contextWrite(this.contextModifier); }`(`DefaultWebClient.java:474-476`)는 **deprecated `RequestHeadersSpec.context(Function<Context, Context>)`만을 위한 배선**이다(선언 `WebClient.java:512-513`, 5.3.2에 deprecated / 백킹 필드 `DefaultWebClient.java:219-220`, 오버라이드 `:349-355`). javadoc이 밝힌 deprecation 사유는 "이 메서드는 다운스트림(중첩·후속) 요청에 context를 제공할 수 없어 가치가 제한적"이라는 것이다. 7.0에서는 이 세 줄과 필드·메서드가 전부 제거된다. **요청에 Reactor Context를 실으려면 `context()`가 아니라 호출 체인 바깥에서 `contextWrite()`를 쓴다** — [../main/09-context-propagation.md](../main/09-context-propagation.md).

```java
private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
        () -> new IllegalStateException("The underlying HTTP client completed without emitting a response."));
```

**⑦ 취소 감지** — `SignalType.CANCEL`이고 응답을 아직 못 받았으면 `observationContext.setAborted(true)`를 세운다. `timeout()`이나 상위 취소로 끊긴 요청을 메트릭에서 구분할 수 있다.

### 3.5 홉 7: ExchangeFilterFunction.apply() — 필터+exchange 접기

```java
// ExchangeFilterFunction.java:71-74
default ExchangeFunction apply(ExchangeFunction exchange) {
    Assert.notNull(exchange, "ExchangeFunction must not be null");
    return request -> this.filter(request, exchange);
}
```

필터 하나와 `ExchangeFunction` 하나가 **새로운 `ExchangeFunction` 하나**로 접힌다. 필터가 여러 개면 `build()`에서 이미 `reduce(andThen)`으로 한 개로 접혀 있으므로(`DefaultWebClientBuilder.java:319-321`), 여기서는 항상 1:1이다. 자세한 체인 구조는 [04-exchange-filter-function.md](04-exchange-filter-function.md).

### 3.6 홉 8: DefaultExchangeFunction.exchange()

`ExchangeFunctions.java:96-115`가 실제 I/O 경계다.

```java
@Override
public Mono<ClientResponse> exchange(ClientRequest clientRequest) {
    Assert.notNull(clientRequest, "ClientRequest must not be null");
    HttpMethod httpMethod = clientRequest.method();
    URI url = clientRequest.url();

    return this.connector
            .connect(httpMethod, url, httpRequest -> clientRequest.writeTo(httpRequest, this.strategies))
            .doOnRequest(n -> logRequest(clientRequest))
            .doOnCancel(() -> logger.debug(clientRequest.logPrefix() + "Cancel signal (to close connection)"))
            .onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, t -> wrapException(t, clientRequest))
            .map(httpResponse -> {
                String logPrefix = getLogPrefix(clientRequest, httpResponse);
                logResponse(httpResponse, logPrefix);
                return new DefaultClientResponse(
                        httpResponse, this.strategies, logPrefix,
                        WebClientUtils.getRequestDescription(httpMethod, url),
                        () -> createRequest(clientRequest));
            });
}
```

**`connector.connect()`에 넘기는 람다가 바디 쓰기의 전부다.** `httpRequest -> clientRequest.writeTo(httpRequest, this.strategies)` — 커넥터가 네이티브 요청 객체를 준비해 콜백하면, `ClientRequest`가 그 위에 자기 상태를 쏟아낸다.

**I/O 실패가 래핑되는 지점이 `onErrorResume` 한 줄이다.**

```java
// WebClientUtils.java:47-48
public static final Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
        t -> !(t instanceof WebClientException) && !(t instanceof CodecException);

// ExchangeFunctions.java:138-140
private <T> Mono<T> wrapException(Throwable t, ClientRequest r) {
    return Mono.error(() -> new WebClientRequestException(t, r.method(), r.url(), r.headers()));
}
```

즉 **`WebClientException`도 `CodecException`도 아닌 모든 예외**가 `WebClientRequestException`으로 감싸진다. DNS 실패, 커넥션 거부, 커넥션 타임아웃, SSL 핸드셰이크 실패, Reactor Netty의 `ReadTimeoutException` — 전부 여기로 수렴한다. 원인은 `getCause()`로 꺼낸다.

`WebClientRequestException`은 메서드·URI·헤더를 들고 있고(`WebClientRequestException.java:37-41`), 헤더는 직렬화 가능하도록 복사된다(`WebClientRequestException.java:55-67`).

### 3.7 홉 9: ClientRequest.writeTo()

`DefaultClientRequestBuilder.java:255-290`:

```java
@Override
public Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies) {
    HttpHeaders requestHeaders = request.getHeaders();
    if (!this.headers.isEmpty()) {
        this.headers.forEach(requestHeaders::putIfAbsent);     // ← putIfAbsent
    }

    MultiValueMap<String, HttpCookie> requestCookies = request.getCookies();
    if (!this.cookies.isEmpty()) {
        this.cookies.forEach((name, values) -> values.forEach(value -> {
            HttpCookie cookie = new HttpCookie(name, value);
            requestCookies.add(name, cookie);
        }));
    }

    request.getAttributes().putAll(this.attributes);

    if (this.httpRequestConsumer != null) {
        this.httpRequestConsumer.accept(request);              // ← 네이티브 요청 접근 지점
    }

    return this.body.insert(request, new BodyInserter.Context() { /* messageWriters, hints */ });
}
```

`putIfAbsent`가 중요하다. **커넥터가 이미 설정한 헤더(예: Reactor Netty의 `Host`, `User-Agent`)는 덮어쓰지 않는다.**

`httpRequestConsumer`는 `RequestHeadersSpec.httpRequest()`로 등록한 콜백이고(`WebClient.java:525`), 바디 쓰기 **직전**에 네이티브 요청을 만질 기회를 준다. 요청별 타임아웃을 거는 전형적 통로다([07-timeout-strategy.md](07-timeout-strategy.md)).

마지막으로 `body.insert()`가 `BodyInserter.Context`를 넘겨받아 `strategies.messageWriters()`로 직렬화한다. `hints`에 `logPrefix`를 담아 코덱 로그도 상관관계를 유지한다(`DefaultClientRequestBuilder.java:286-288`).

### 3.8 홉 10-12: 응답 래핑과 바디 처리

`DefaultClientResponse`가 `ClientHttpResponse`를 감싼다. `bodyToMono()`는 `BodyExtractors`에 위임하고(`DefaultClientResponse.java:148-151`), 모든 바디 추출에 `checkpoint`를 붙인다(`DefaultClientResponse.java:132-146`).

```java
public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
    T result = extractor.extract(this.response, this.bodyExtractorContext);
    String description = "Body from " + this.requestDescription + " [DefaultClientResponse]";
    if (result instanceof Mono<?> mono) {
        return (T) mono.checkpoint(description);
    }
    ...
}
```

`DefaultResponseSpec.bodyToMono()`가 마지막 홉이다(`DefaultWebClient.java:579-584`).

```java
@Override
public <T> Mono<T> bodyToMono(Class<T> elementClass) {
    Assert.notNull(elementClass, "Class must not be null");
    return this.responseMono.flatMap(response ->
            handleBodyMono(response, response.bodyToMono(elementClass)));
}
```

```java
// DefaultWebClient.java:663-667
private <T> Mono<T> handleBodyMono(ClientResponse response, Mono<T> body) {
    body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));
    Mono<T> result = applyStatusHandlers(response);
    return (result != null ? result.switchIfEmpty(body) : body);
}
```

읽는 순서가 반대다. **먼저 상태 핸들러를 돌리고(`applyStatusHandlers`), 매칭되는 게 없으면(`null`) 바디 디코딩으로 넘어간다.** 매칭되는 핸들러가 있어도 그 핸들러가 `Mono.empty()`를 반환하면 `switchIfEmpty`로 정상 바디 처리로 떨어진다 — 이것이 "에러 상태를 정상 응답으로 처리하기"의 메커니즘이다(`WebClient.java:815-817`).

상태 핸들러의 구조와 순서는 [03-retrieve-vs-exchange.md](03-retrieve-vs-exchange.md)에서 다룬다.

---

## 4. 실전 예제

### 4.1 assembly time / subscribe time 차이를 눈으로 확인

```java
@Test
void monoIsColdAndRepeatsOnEachSubscribe() {
    AtomicInteger hits = new AtomicInteger();   // 요청 수를 세는 테스트 서버를 띄웠다고 가정

    Mono<Foo> mono = webClient.get().uri("/foo/{id}", 42)
            .retrieve().bodyToMono(Foo.class);

    assertThat(hits).hasValue(0);   // 조립만 했으므로 요청 0건
    mono.block();
    assertThat(hits).hasValue(1);
    mono.block();                   // deferContextual 람다가 다시 실행된다
    assertThat(hits).hasValue(2);   // 요청 2건 — 캐시되지 않는다
}
```

결과를 공유하려면 `.cache()`를 붙인다. 이 "구독마다 재실행" 성질이 `retry()`를 가능하게 하는 동시에, `Mono`를 캐시 없이 여러 곳에 넘기면 중복 호출을 일으킨다.

### 4.2 I/O 실패와 HTTP 실패를 구분해 재시도

`WebClientRequestException`(전송 실패)과 `WebClientResponseException`(HTTP 상태)이 서로 다른 가지라는 점을 활용한다.

```java
package com.example.payment;

import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class PaymentClient {

    private final WebClient webClient;
    private final PaymentRetryProperties retryProps;

    public PaymentClient(WebClient paymentWebClient, PaymentRetryProperties retryProps) {
        this.webClient = paymentWebClient;
        this.retryProps = retryProps;
    }

    public Mono<PaymentResult> fetch(String paymentId) {
        return this.webClient.get()
                // URI 템플릿을 유지해야 메트릭 카디널리티가 터지지 않는다
                // (DefaultWebClient.java:229 에서 URI_TEMPLATE_ATTRIBUTE 기록)
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .bodyToMono(PaymentResult.class)
                .retryWhen(Retry.backoff(retryProps.maxAttempts(), retryProps.firstBackoff())
                        .maxBackoff(retryProps.maxBackoff())
                        .jitter(0.5d)
                        .filter(PaymentClient::isRetryable));
    }

    private static boolean isRetryable(Throwable ex) {
        // 전송 계층 실패: 커넥션 거부, DNS, 타임아웃 — ExchangeFunctions.java:106 에서 래핑됨.
        // 요청이 서버에 닿지 않았을 가능성이 높으므로 재시도 안전.
        if (ex instanceof WebClientRequestException) {
            return true;
        }
        // HTTP 상태 실패: 5xx와 429만 재시도. 4xx는 재시도해도 같은 결과다.
        if (ex instanceof WebClientResponseException responseEx) {
            return responseEx.getStatusCode().is5xxServerError()
                    || responseEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
        }
        return false;
    }
}
```

```java
@ConfigurationProperties("app.payment.retry")
public record PaymentRetryProperties(
        long maxAttempts,
        Duration firstBackoff,
        Duration maxBackoff) {
}
```

> 주의: 비멱등 요청(POST로 결제 생성)에 `WebClientRequestException` 재시도를 그대로 적용하면 중복 결제가 날 수 있다. 요청이 서버에 닿은 뒤 응답만 못 받은 경우가 `WebClientRequestException`에 섞여 들어오기 때문이다. 멱등 키를 함께 보내야 한다. 전체 복원력 전략은 [09-resilience-integration.md](09-resilience-integration.md).

### 4.3 httpRequest()로 요청별 네이티브 설정

`writeTo()`가 바디 쓰기 직전에 `httpRequestConsumer`를 호출한다는 사실을 이용한다(`DefaultClientRequestBuilder.java:272-274`).

```java
public Mono<Report> fetchLargeReport(String reportId, Duration responseTimeout) {
    return this.webClient.get()
            .uri("/reports/{reportId}", reportId)
            .httpRequest(request -> {
                // 커넥터가 Reactor Netty일 때만 유효. 다른 커넥터면 Optional이 비어 있다.
                HttpClientRequest nativeRequest = request.getNativeRequest();
                nativeRequest.responseTimeout(responseTimeout);
            })
            .retrieve()
            .bodyToMono(Report.class);
}
```

`getNativeRequest()`는 커넥터 구현에 따라 타입이 다르므로, 이 코드는 커넥터를 바꾸면 깨진다. 전역 타임아웃으로 해결 가능하면 그쪽을 우선한다.

### 4.4 디버깅: checkpoint를 읽는 법

파이프라인은 두 곳에 `checkpoint`를 심어 둔다.

- `DefaultWebClient.java:470-472` — `"Request to GET https://api.example.com/payments/42 [DefaultWebClient]"`
- `DefaultClientResponse.java:136` — `"Body from GET https://api.example.com/payments/42 [DefaultClientResponse]"`
- `DefaultWebClient.java:704-706` — `"404 NOT_FOUND from GET … [DefaultWebClient]"`

`WebClientUtils.getRequestDescription()`은 쿼리 스트링이 있으면 scheme·host·port·path만으로 URI를 재구성한다(`WebClientUtils.java:75-85`). 토큰이 쿼리 파라미터로 들어간 경우 로그 유출을 막는 안전장치다.

**다만 v6.2.19 구현에는 빈틈이 있다.** 재구성이 `if (StringUtils.hasText(uri.getQuery()))` 가드 **안에서만** 일어나므로, 쿼리가 없는 URL은 원본이 그대로 찍힌다. 즉 `https://user:secret@api.example.com/v1/foo#frag` 처럼 **쿼리 없이 userInfo나 fragment만 있는 URL은 자격증명이 로그에 노출된다.** 7.0에서 이 구현이 더 안전하게 재작성되었다([12-boot4-spring7-migration.md](12-boot4-spring7-migration.md)). 6.2에서는 **자격증명을 URL에 넣지 않는 것**이 유일한 방어다 — 인증은 헤더로 보낸다.

리액티브 스택트레이스는 호출 지점을 잃기 쉬운데, 이 체크포인트가 **어떤 요청에서 터졌는지**를 알려준다. 더 많은 컨텍스트가 필요하면 `Hooks.onOperatorDebug()`나 `ReactorDebugAgent`를 켠다 — 자세한 내용은 [../main/02-assembly-vs-subscribe-time.md](../main/02-assembly-vs-subscribe-time.md).

---

## 5. 정리

- `get()`부터 `bodyToMono()`까지의 체인 전체가 **조립**이다. 단 하나의 예외는 `initRequestBuilder()` 호출로, `exchange()`의 `Mono.deferContextual` **바깥**에서 실행된다(`DefaultWebClient.java:451`).
- Observation 시작, 필터 체인 합성, `ClientRequest` 확정은 **모두 구독 시점**이고 **구독마다 새로** 일어난다(`DefaultWebClient.java:452-490`). 같은 `Mono`를 두 번 구독하면 요청이 두 번 나간다.
- 필터 합성은 `filterFunctions.andThen(ObservationFilterFunction)`이므로 **사용자 필터가 바깥, 관측 필터가 안쪽**이다(`DefaultWebClient.java:458-461`). 메트릭에 기록되는 응답은 사용자 필터 가공 전의 원본이다.
- `ClientRequest`는 `build()` 시점에 헤더·쿠키·attribute가 모두 읽기 전용으로 잠긴다(`DefaultClientRequestBuilder.java:204-207`). 필터에서 수정하려면 `ClientRequest.from(request)`를 쓴다.
- 실제 I/O는 `connector.connect(method, url, req -> clientRequest.writeTo(req, strategies))` 한 줄이다(`ExchangeFunctions.java:102-103`). 바디 직렬화는 커넥터가 아니라 `ClientRequest.writeTo()`의 책임이다.
- **I/O 실패는 `WebClientRequestException`으로 래핑된다** — `WebClientException`도 `CodecException`도 아닌 모든 예외가 대상이다(`ExchangeFunctions.java:106`, `WebClientUtils.java:47-48`). HTTP 상태 오류인 `WebClientResponseException`과 구분해서 재시도 정책을 짠다.
- `uri(String, ...)` 오버로드만 `URI_TEMPLATE_ATTRIBUTE`를 기록한다. `uri(URI)`나 `uri(Function)`을 쓰면 메트릭에서 URI 템플릿을 잃는다.
- `handleBodyMono()`는 **상태 핸들러를 먼저, 바디 디코딩을 나중에** 적용한다(`DefaultWebClient.java:663-667`).

---

## 관련 문서
- 선행: [WebClient Builder 구조와 커넥터 선택](01-webclient-builder-connectors.md), [Assembly time vs Subscribe time](../main/02-assembly-vs-subscribe-time.md)
- 후행: [retrieve vs exchangeToMono와 에러 처리](03-retrieve-vs-exchange.md)
- 참고: [필터 체인](04-exchange-filter-function.md), [Context 전파](../main/09-context-propagation.md), [관측성](10-observability.md)

---
*참고: Spring Framework 6.2.19 (Spring Boot 3.x) 기준*
