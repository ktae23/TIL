# Spring Boot 4 / Framework 7 마이그레이션 — WebClient 관점

Spring Framework 6.2.19에서 7.0.9로 올릴 때 WebClient 사용 코드가 실제로 깨지는 지점과 새로 생긴 선택지를, 양쪽 소스를 직접 비교해 정리했다. **두 워크트리에서 확인된 항목만** 담았고, 확인되지 않은 항목은 §5.2에 따로 적었다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

```
┌─ A. 제거 (컴파일 에러) ─────────────────────────────────────┐
│   exchange() / syncBody() / context()                      │
│   basicAuthentication() 무인자 + Credentials               │
│   ReactorNetty2ClientHttpConnector 계열                     │
│   http.client.reactive.ReactorResourceFactory              │
│   ClientRequestObservationContext() 무인자 생성자            │
├─ B. deprecated (경고, 아직 동작) ──────────────────────────┤
│   Jackson2JsonDecoder / Jackson2JsonEncoder                │
│   CodecConfigurer.jackson2JsonDecoder / …Encoder           │
│   WebClientResponseException.UnprocessableEntity           │
│   (Reactor) reactor.util.annotation.Nullable / NonNull…    │
├─ C. 신규 기능 (쓸지 선택) ─────────────────────────────────┤
│   @ImportHttpServices + HttpServiceProxyRegistry           │
│   defaultApiVersion / apiVersionInserter / apiVersion       │
│   NotFoundWebClientAdapterDecorator                        │
│   basicAuthentication(String, String, Charset)             │
│   JacksonJsonDecoder / JacksonJsonEncoder (Jackson 3)      │
│   (netty) maxLifeTimeVariance / httpAuthentication / …     │
├─ D. 변화 없음 (안심) ──────────────────────────────────────┤
│   Observation 이름·태그 전체                                │
│   ExchangeFilterFunction / ExchangeFunction 시그니처        │
│   ClientRequest / ClientResponse 빌더                       │
│   Reactor retry·Schedulers 공개 API                         │
│   HttpClientMetricsRecorder 인터페이스                      │
└────────────────────────────────────────────────────────────┘
```

| 라이브러리 | before | after |
|---|---|---|
| Spring Framework | 6.2.19 | 7.0.9 |
| Reactor Core | 3.7.19 | 3.8.7 |
| reactor-netty | 1.2.18 | 1.3.7 |
| Reactor BOM | 2024.0.18 (`reactor-netty-1.2/gradle.properties:7`) | 2025.0.7 (`reactor-netty-1.3/gradle.properties:6`) |

---

## 2. 왜 알아야 하는가 (Why)

### 2.1 `exchange()` 는 5.3에서 예고됐고 7.0에서 실행됐다

6.2.19의 deprecation 문구 원문 (`v6.2.19 WebClient.java:637-644`):

> `@deprecated` since 5.3 due to the possibility to leak memory and/or connections; please, use {@link #exchangeToMono(Function)}, {@link #exchangeToFlux(Function)}; consider also using {@link #retrieve()} which provides access to the response status and headers via {@link ResponseEntity} along with error status handling.

같은 Javadoc의 누수 경고 (`:626-631`):

> **NOTE:** Unlike {@link #retrieve()}, when using {@code exchange()}, it is the responsibility of the application to consume any response content regardless of the scenario (success, error, unexpected data, etc). Not doing so can cause a memory leak.

7년 가까이 예고된 제거이므로, 6.2에 머물러 있는 동안 전부 치워 두는 게 정석이다. 치우는 방법은 `exchange()` 가 왜 필요했는지에 따라 갈린다([`03-retrieve-vs-exchange.md`](03-retrieve-vs-exchange.md)).

### 2.2 Jackson 3는 **다른 좌표의 다른 패키지**다

클래스 이름만 바뀐 게 아니다. 감지하는 클래스 자체가 다르다.

```java
// v6.2.19 BaseDefaultCodecs.java:106
jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) && …

// v7.0.9 BaseDefaultCodecs.java:130-132
JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) && …
```

Jackson 3의 루트 패키지는 **`tools.jackson`** 이다. 즉 Jackson 2와 **클래스패스에 공존할 수 있다**. 7.0.9는 둘 다 지원하고 Jackson 3를 우선한다(§3.2). 그래서 마이그레이션이 한 번에 끊기지 않고, **"의존성을 바꾸는 순간 기본 코덱이 조용히 교체된다"**는 다른 종류의 위험이 생긴다. 커스텀 `ObjectMapper` 를 주입해 쓰고 있었다면 그 설정이 적용되지 않는 상태로 돌아갈 수 있다.

### 2.3 `@ImportHttpServices` 는 WebClient를 "그룹"으로 다룬다

6.2에서는 `@HttpExchange` 인터페이스 클라이언트를 서비스마다 손으로 만들어야 했다.

```java
// 6.2 방식
HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient)).build();
OrderApi orderApi = factory.createClient(OrderApi.class);
```

`org.springframework.web.service.registry` 패키지는 **6.2.19에 존재하지 않는다** — `web/service/` 아래에 `annotation`, `invoker` 만 있다. 7.0.9에서 `registry` 가 추가되고 그 안의 모든 타입이 `@since 7.0` 이다.

```java
// 7.0 방식
@Configuration
@ImportHttpServices(group = "order", clientType = ClientType.WEB_CLIENT,
        types = {OrderApi.class, PaymentApi.class})
class HttpServicesConfig { }
```

이득은 **그룹별 공통 설정**이다. 같은 그룹의 모든 인터페이스가 하나의 `WebClient.Builder` 를 공유하므로 baseUrl·타임아웃·필터·관측 설정을 한곳에서 준다.

---

## 3. 내부 구현 분석 (How)

### 3.1 제거된 WebClient API

`WebClient.java` 공개 시그니처를 양쪽에서 비교한 결과 **제거 3건 / 추가 3건**이다.

| 항목 | 6.2.19 상태 | 7.0.9 상태 | 대응 |
|---|---|---|---|
| `RequestHeadersSpec.exchange()` | `@Deprecated` 존재 (`WebClient.java:644-645`) | **제거**. 내부에만 `private Mono<ClientResponse> exchange()` 로 남음 (`DefaultWebClient.java:442`) | `retrieve()` 또는 `exchangeToMono` / `exchangeToFlux` |
| `RequestBodySpec.syncBody(Object)` | `@Deprecated` (`:798-799`)<br/>원문: *"deprecated as of Spring Framework 5.2 in favor of `bodyValue(Object)`"* | **제거** | `bodyValue(Object)` |
| `RequestHeadersSpec.context(Function<Context,Context>)` | `@Deprecated` (`:512-513`)<br/>원문: *"deprecated in 5.3.2 to be removed soon after; this method cannot provide context to downstream (nested or subsequent) requests and is of limited value"* | **제거** | 호출 결과에 `.contextWrite(...)` |
| `Builder.defaultApiVersion(Object)` | 없음 | 추가, `@since 7.0` (`WebClient.java:262`) | — |
| `Builder.apiVersionInserter(@Nullable ApiVersionInserter)` | 없음 | 추가, `@since 7.0` (`:274`) | — |
| `RequestHeadersSpec.apiVersion(@Nullable Object)` | 없음 | 추가, `@since 7.0` (`:515`) | — |

`ApiVersionInserter` 는 webflux가 아니라 **`org.springframework.web.client`** (spring-web)에 있다 (`v7.0.9 .../web/client/ApiVersionInserter.java:43`) — `RestClient` 와 공유하는 추상이다. 6.2.19에는 이 타입이 없다. 정적 팩토리로 삽입 위치를 고른다: `useHeader(@Nullable String)` (`:70`), `useQueryParam(@Nullable String)` (`:78`), `usePathSegment(@Nullable Integer)` (`:94`), `builder()` (`:101`).

`apiVersion(Object)` Javadoc이 동작 규칙을 명시한다 (`v7.0.9 WebClient.java:501-514`):

> If no version is set, the {@link Builder#defaultApiVersion(Object) defaultApiVersion} is used, if configured. If {@code null} is passed, then an API version is not inserted irrespective of default version settings.

즉 `apiVersion(null)` 은 "기본 버전도 넣지 말라"는 명시적 거부다.

### 3.2 JSON 코덱 이동

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| 기본 JSON 디코더/인코더 | `Jackson2JsonDecoder` / `Jackson2JsonEncoder` | `JacksonJsonDecoder` / `JacksonJsonEncoder` `@since 7.0` |
| 구 클래스 상태 | 정상 (deprecation 없음) | `@Deprecated(since = "7.0", forRemoval = true)` |
| 베이스 클래스 | `AbstractJackson2Decoder`/`…Encoder` (`codec/json/`) | `AbstractJacksonDecoder<JsonMapper>`/`…Encoder` (`codec/` 루트로 승격) |
| 추가 코덱 | — | `GsonDecoder`/`GsonEncoder` `@since 7.0` |

```java
// v7.0.9 Jackson2JsonDecoder.java:45-51
 * @deprecated since 7.0 in favor of {@link AbstractJacksonDecoder}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2JsonDecoder extends AbstractJackson2Decoder {

// v7.0.9 JacksonJsonDecoder.java:41-49
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/FasterXML/jackson">Jackson 3.x</a> leveraging non-blocking parsing.
 * @since 7.0
 */
public class JacksonJsonDecoder extends AbstractJacksonDecoder<JsonMapper> {
```

선택 로직은 **Jackson 3 우선, 없으면 Jackson 2 폴백** (`v7.0.9 BaseDefaultCodecs.java:871-886`):

```java
protected Decoder<?> getJacksonJsonDecoder() {
    if (this.jacksonJsonDecoder == null) {
        if (JACKSON_PRESENT)        { this.jacksonJsonDecoder = new JacksonJsonDecoder(); }
        else if (JACKSON_2_PRESENT) { this.jacksonJsonDecoder = new Jackson2JsonDecoder(); }
        else                        { throw new IllegalStateException("Jackson not present"); }
    }
    return this.jacksonJsonDecoder;
}
```

구 접근자는 새 것으로 위임하도록 남았다 (`:889-894`, `@Deprecated` + *"in favor of {@link #getJacksonJsonDecoder()}"*).

`CodecConfigurer.DefaultCodecs` 쪽:

| 메서드 | 6.2.19 | 7.0.9 |
|---|---|---|
| `jacksonJsonDecoder(Decoder<?>)` / `jacksonJsonEncoder(Encoder<?>)` | 없음 | 추가, `@since 7.0` (`CodecConfigurer.java:121`) |
| `jackson2JsonDecoder` / `jackson2JsonEncoder` / `jackson2SmileDecoder` / `jackson2SmileEncoder` | 추상 메서드 | `default` + `@Deprecated(since="7.0", forRemoval=true)` (`:129-132`)<br/>원문: *"deprecated in favor of {@link #jacksonJsonDecoder(Decoder)}"* |
| `gsonDecoder`/`gsonEncoder`, `jacksonCborDecoder`/`Encoder`, `jacksonSmileDecoder`/`Encoder`, `jacksonXmlDecoder`/`Encoder` | 없음 | 추가 |

추상 메서드가 `default` 로 내려간 덕분에 `DefaultCodecs` 를 직접 구현한 코드는 깨지지 않는다 — 호출하는 쪽만 경고를 받는다.

### 3.3 `@ImportHttpServices` 와 그룹 어댑터

```
v6.2.19                 v7.0.9
web/service/            web/service/
  ├─ annotation/          ├─ annotation/
  └─ invoker/             ├─ invoker/
                          └─ registry/     ← 신규, 전부 @since 7.0
                               AbstractHttpServiceRegistrar, GroupsMetadata,
                               HttpServiceGroup, HttpServiceGroupAdapter,
                               HttpServiceGroupConfigurer, HttpServiceProxyRegistry,
                               HttpServiceProxyRegistryFactoryBean, ImportHttpServices,
                               ImportHttpServiceRegistrar,
                               HttpServiceProxyBeanRegistrationAotProcessor
```

`@ImportHttpServices` 속성 (`v7.0.9 ImportHttpServices.java`):

| 속성 | 기본값 | 의미 |
|---|---|---|
| `value()` / `types()` | `{}` | 등록할 `@HttpExchange` 인터페이스 (서로 `@AliasFor`) |
| `group()` | `HttpServiceGroup.DEFAULT_GROUP_NAME` = `"default"` | 그룹 이름 |
| `basePackageClasses()` / `basePackages()` | `{}` | 인터페이스 자동 탐색 |
| `clientType()` | `ClientType.UNSPECIFIED` | `REST_CLIENT` / `WEB_CLIENT` / `UNSPECIFIED` |

`@Repeatable(ImportHttpServices.Container.class)` + `@Import(ImportHttpServiceRegistrar.class)` 이므로 한 `@Configuration` 에 여러 번 붙여 그룹을 여럿 만들 수 있다.

**기본 클라이언트는 `RestClient` 다** (`v7.0.9 ImportHttpServices.java:90-95`):

> By default, this is {@link HttpServiceGroup.ClientType#UNSPECIFIED} in which case {@code RestClient} is used, but this default can be changed via {@link AbstractHttpServiceRegistrar#setDefaultClientType}.

WebClient를 쓰려면 **반드시 `clientType = ClientType.WEB_CLIENT` 를 명시**해야 한다. 안 쓰면 블로킹 `RestClient` 프록시가 만들어져 WebFlux 이벤트 루프에서 블로킹이 일어난다.

어댑터 탐색은 `spring.factories` 가 아니라 **클래스 이름 문자열 + `ClassUtils.forName`** 이다 (`v7.0.9 HttpServiceProxyRegistryFactoryBean.GroupAdapterInitializer`, `:140-166`):

```java
private static void addGroupAdapter(..., HttpServiceGroup.ClientType clientType, String className) {
    try {
        Class<?> clazz = ClassUtils.forName(className, HttpServiceGroupAdapter.class.getClassLoader());
        groupAdapters.put(clientType, (HttpServiceGroupAdapter<?>) BeanUtils.instantiateClass(clazz));
    }
    catch (ClassNotFoundException ignored) {
    }
}
```

**`spring-webflux` 가 클래스패스에 없으면 `WEB_CLIENT` 어댑터가 조용히 등록되지 않는다**(`ClassNotFoundException` 을 무시한다).

어댑터 본문 (`v7.0.9 WebClientHttpServiceGroupAdapter.java:31-47`):

```java
public class WebClientHttpServiceGroupAdapter implements HttpServiceGroupAdapter<WebClient.Builder> {
    @Override public WebClient.Builder createClientBuilder() { return WebClient.builder(); }
    @Override public Class<? extends HttpServiceGroupConfigurer<WebClient.Builder>> getConfigurerType() {
        return WebClientHttpServiceGroupConfigurer.class;
    }
    @Override public HttpExchangeAdapter createExchangeAdapter(WebClient.Builder clientBuilder) {
        return WebClientAdapter.create(clientBuilder.build());
    }
}
```

`WebClient.builder()` 를 **맨 처음부터** 만든다 — 애플리케이션이 커스터마이즈한 `WebClient.Builder` 빈을 쓰지 않는다. 설정은 `WebClientHttpServiceGroupConfigurer` 빈으로 주입해야 한다(§4.2).

설정 DSL은 `HttpServiceGroupConfigurer<CB>` 이고, WebClient용 하위 타입이 `WebClientHttpServiceGroupConfigurer extends HttpServiceGroupConfigurer<WebClient.Builder>` (`:29`):

| 메서드 | 줄 | 역할 |
|---|---|---|
| `Groups<CB> filterByName(String...)` | `:59` | 그룹 이름으로 범위 좁히기 |
| `Groups<CB> filter(Predicate<HttpServiceGroup>)` | `:64` | 임의 조건으로 좁히기 |
| `void forEachClient(ClientCallback<CB>)` | `:71` | `withClient(group, clientBuilder)` — 빌더 커스터마이즈 |
| `void forEachClient(InitializingClientCallback<CB>)` | `:78` | `initClient(group)` — 빌더를 **직접 제공** |
| `void forEachProxyFactory(ProxyFactoryCallback)` | `:85` | `withProxyFactory(group, HttpServiceProxyFactory.Builder)` |

`forEachProxyFactory` 로 받은 빌더에 `exchangeAdapterDecorator(Function<HttpExchangeAdapter, HttpExchangeAdapter>)` (`v7.0.9 HttpServiceProxyFactory.java:178`) 를 걸면 §3.4의 404 데코레이터를 그룹 단위로 적용할 수 있다.

조회 API (`v7.0.9 HttpServiceProxyRegistry.java`): `<P> P getClient(Class<P>)` (전체 그룹에서 유일할 때만), `<P> P getClient(String groupName, Class<P>)`, `Set<String> getGroupNames()`, `Set<Class<?>> getClientTypesInGroup(String)`.

### 3.4 `NotFoundWebClientAdapterDecorator`

6.2.19에 없고 7.0.9의 `…/function/client/support/` 에 `@since 7.0` 으로 추가됐다. 404를 예외가 아니라 "없음"으로 바꿔 준다 (`extends ReactorHttpExchangeAdapterDecorator`, `:40`).

| 메서드 | 404일 때 결과 | 줄 |
|---|---|---|
| `exchangeForBody(...)` | `null` | `:48-55` |
| `exchangeForBodilessEntity(...)` / `exchangeForEntity(...)` | `ResponseEntity.notFound().build()` | `:57-75` |
| `exchangeForBodyMono(...)` | **빈 `Mono`** | `:77-80` |
| `exchangeForBodyFlux(...)` | 빈 `Flux` | `:82-86` |

잡는 예외는 `WebClientResponseException.NotFound` 하나다 (`:77-80`):

```java
return super.exchangeForBodyMono(values, bodyType).onErrorResume(
        WebClientResponseException.NotFound.class, ex -> Mono.empty());
```

6.2에서는 호출마다 `.onStatus(NOT_FOUND::equals, r -> r.releaseBody().then(Mono.empty()))` 를 붙여야 했다.

### 3.5 `basicAuthentication` 오버로드

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `basicAuthentication(String, String)` | 존재 (`ExchangeFilterFunctions.java:91`) | 존재 (`:91`), ISO-8859-1 사용 명시 |
| `basicAuthentication(String, String, Charset)` | **없음** | 존재, **`@since 7.0.8`** (`:104-109`) |
| `basicAuthentication()` (무인자) | 존재, `@Deprecated` (`:112-113`)<br/>원문: *"deprecated as of Spring 5.1 in favor of using `HttpHeaders#setBasicAuth(String, String)` while building the request"* | **제거** |
| `ExchangeFilterFunctions.Credentials` | 존재, `@Deprecated` (`:133`) | **제거** |
| `BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE` | 존재 (`:47-48`) | **제거** |

```java
// v7.0.9 ExchangeFilterFunctions.java:104-115
 * @param charset the charset to use to convert the credentials into an octet
 * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
 * @since 7.0.8
public static ExchangeFilterFunction basicAuthentication(String username, String password, @Nullable Charset charset) {
    String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
    return (request, next) -> next.exchange(ClientRequest.from(request)
                    .headers(headers -> headers.setBasicAuth(encodedCredentials)).build());
}
```

**`@since 7.0.8` 이므로 7.0.9에 들어 있다** — 워크트리에서 확인했다. 7.0.0~7.0.7에는 없다. UTF-8 비밀번호가 필요하면 이 오버로드가 유일한 공식 경로다. 또 하나: 자격증명 인코딩을 **필터 생성 시점에 한 번**만 수행하므로 요청마다 Base64 인코딩이 돌지 않는다.

### 3.6 예외 클래스와 커넥터

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `WebClientResponseException.UnprocessableEntity` | 정상 (`:487`) | `@Deprecated(since = "7.0")` (`:491`)<br/>원문: *"deprecated since 7.0 in favor of {@link UnprocessableContent}"* |
| `WebClientResponseException.UnprocessableContent` | 없음 | 신규 — `HttpStatus.UNPROCESSABLE_CONTENT` 기반 |
| `ReactorNetty2ClientHttpConnector`/`…Request`/`…Response`/`ReactorNetty2ResourceFactory` | 존재(테스트에서 `@SuppressWarnings("removal")` 로 사용) | **제거** |
| `http.client.reactive.ReactorResourceFactory` | 존재, `@Deprecated(since = "6.1")` (`:43-45`)<br/>원문: *"deprecated since 6.1 due to a package change; use {@link org.springframework.http.client.ReactorResourceFactory} instead"* | **제거** |
| `http.client.ReactorResourceFactory` | 존재, `@since 6.1` | 존재 (유일한 위치) |
| `JdkResponseCookieParser` | 없음 | 신규 |
| `ReactorClientHttpConnector`, `JdkClientHttpConnector`, `JettyClientHttpConnector`, `HttpComponentsClientHttpConnector` | 존재 | 존재 (변화 없음) |

HTTP 422의 reason phrase가 RFC 9110에서 "Unprocessable Content"로 바뀐 것을 따라간 이름 변경이다. 상태코드로 분기하던 코드는 영향이 없다. `ReactorNetty2*` 는 Netty 5 프리뷰용 실험 코드였고 Netty 5가 중단되면서 함께 사라졌다.

### 3.7 Observation은 바뀌지 않았다

양쪽 `DefaultClientRequestObservationConvention.java` 의 `KeyValue.of(...)` 호출을 전부 비교한 결과 **차이가 없다**. 관측 이름 `http.client.requests`, low cardinality 태그 `method`/`uri`/`status`/`client.name`/`exception`/`outcome`, high cardinality 태그 `http.url`, 값 `IO_ERROR`/`CLIENT_ERROR`/`SUCCESS`/`UNKNOWN` 모두 동일하다. **대시보드와 알림 쿼리를 손댈 필요가 없다**([`10-observability.md`](10-observability.md)).

깨지는 것은 하나뿐이다.

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `ClientRequestObservationContext()` 무인자 | 존재, `@Deprecated(since = "6.1.2", forRemoval = true)`<br/>원문: *"deprecated as of 6.1.2, in favor of {@link #ClientRequestObservationContext(ClientRequest.Builder)}"* | **제거** |
| `ClientRequestObservationContext(ClientRequest.Builder)` | 존재, `@since 6.1.2` | 존재 (유일) |
| nullability 어노테이션 | `org.springframework.lang.Nullable` | **`org.jspecify.annotations.Nullable`** (`:20`) |

### 3.8 Spring 7의 jspecify 전환

7.0.9의 webflux 클라이언트 클래스가 jspecify를 쓴다 — 예: `ClientRequestObservationContext.java:20`, `NotFoundWebClientAdapterDecorator.java:19`. 6.2.19는 `org.springframework.lang.Nullable` 이다. jspecify는 **타입 사용 어노테이션**이라 선언 위치가 다르다:

```java
// 6.2.19 스타일                        // 7.0.9 스타일
@Nullable                               public <T> @Nullable T exchangeForBody(...)
public String getUriTemplate()
```

Kotlin에서 호출할 때 플랫폼 타입 추론이 더 정확해진다. Java 코드에서는 정적분석기 경고 양상이 달라질 수 있지만 **컴파일은 깨지지 않는다**.

### 3.9 Reactor Core 3.7.19 → 3.8.7

| 항목 | 3.7.19 | 3.8.7 |
|---|---|---|
| jspecify 사용 파일 수 (main) | **0개** | **393개** |
| `reactor.util.annotation.Nullable` | 정상 | `@Deprecated` — 원문: *"Use {@link org.jspecify.annotations.Nullable} instead. To be removed in …"* (`:49`, `:57`) |
| `reactor.util.annotation.NonNullApi` | 정상 | `@Deprecated` — 원문: *"Use {@link org.jspecify.annotations.NullMarked} instead. To be removed …"* (`:45`, `:53`) |
| `reactor.util.annotation.NonNull` | 정상 | `@Deprecated` |
| `reactor.util.retry` 공개 시그니처 | — | **변화 없음** (`RetryBackoffSpec`/`RetrySpec` 전체 메서드 diff 동일) |
| `Schedulers` 공개 정적 메서드 | — | **변화 없음** |
| `@since 3.8` 로 표시된 새 공개 API | — | **main 소스에 없음** |

WebClient 사용자 입장에서 Reactor 3.8.7의 유일한 가시적 변화는 **null 어노테이션의 jspecify 전환**이다. `reactor.util.annotation.Nullable` import는 `org.jspecify.annotations.Nullable` 로, 패키지 수준 `@NonNullApi` 는 `@NullMarked` 로 바꾼다.

### 3.10 reactor-netty 1.2.18 → 1.3.7

| 항목 | 1.2.18 | 1.3.7 | `@since` |
|---|---|---|---|
| `ConnectionProvider.Builder.evictInBackground(Duration)` | 존재 (`:762`) | 존재 (`:796`) | — |
| `…evictInBackground(Duration, Scheduler)` | **없음** | 존재 (`:817`) | **1.3.2** |
| `…maxLifeTimeVariance(double)` | **없음** | 존재 (`:684`) | **1.3.4** |
| `MeterRegistrar.recordPendingAcquireSuccess(...)` / `…Failure(...)` | **없음** | default 메서드 | **1.3.7** |
| `HttpClient.httpAuthentication(BiPredicate, BiConsumer[, int])` | **없음** | 존재 (`:1789`, `:1843`) | **1.3.1** |
| `HttpClient.httpAuthenticationWhen(BiPredicate, BiFunction[, int])` | **없음** | 존재 | **1.3.1** |
| `HttpClientAuthenticationException` | **없음** | 존재 | **1.3.1** |
| `Http2AllocationStrategy.Builder.strictConnectionReuse(boolean)` | **없음** | 존재 (`:89`) | **1.3.3** |
| `Http2AllocationStrategy.Builder.streamBatchSize(int)` | **없음** | 존재 (`:111`) | **1.3.4** |
| WebSocket 클라이언트 메트릭 (`WebSocketClientMetricsRecorder`, `MicrometerWebSocketClientMetricsRecorder`, `WebSocketClientMeters`, `WebSocketClientObservations` 등) | **없음** | 존재 | **1.3.7** |
| `HttpClientMetricsRecorder` 인터페이스 | — | **변화 없음** (메서드 diff 동일) | — |
| `HttpClient` 새 deprecation | — | **없음** (`@Deprecated` 위치가 1.2와 동일한 5곳) | — |

`maxLifeTimeVariance(double)` Javadoc (`v1.3.7 ConnectionProvider.java:662-683`):

> Set a variance percentage for {@link #maxLifeTime(Duration)}, introducing per-connection jitter to prevent simultaneous expiry of connections created around the same time. The effective max lifetime for each connection will be in the range: `[maxLifeTime * (1 - variance/100), maxLifeTime]` … with a {@code maxLifeTime} of 60 seconds and a variance of 2.5%, connections would expire between 58.5s and 60s. … Defaults to 0 (no variance).

[`08-connection-reuse-pitfalls.md`](08-connection-reuse-pitfalls.md) 에서 다루는 "풀 전체가 동시에 만료돼 커넥션 폭풍이 나는" 문제의 1.3 전용 해법이다.

`httpAuthentication(...)` 은 주의가 필요하다 — **reactor-netty 층에서 또 하나의 자동 재시도가 생긴다.** 401을 받으면 인증 헤더를 붙여 다시 보낸다(기본 `maxRetries = 1`, `v1.3.7 HttpClient.java:1789-1792`). [`09-resilience-integration.md`](09-resilience-integration.md) §2.3의 "시도 횟수가 곱으로 늘어난다" 문제가 **한 겹 더** 생긴다. WebClient에서는 같은 일을 `ExchangeFilterFunction` 으로 하는 편이 관측·테스트가 쉽다.

`recordPendingAcquireSuccess`/`…Failure` 는 커스텀 `MeterRegistrar` 가 **대기 지연**을 받게 해 준다. 내부 브리지 클래스 Javadoc (`v1.3.7 MeterRegistrarPoolMetricsRecorder.java:24-29`):

> A {@link PoolMetricsRecorder} that bridges pending acquisition latencies to a user-provided {@link MeterRegistrar}. This allows custom registrars to receive the {@code pending.streams.time} data that is otherwise only available to the built-in Micrometer integration.

1.2에서는 커스텀 레지스트라가 이 데이터를 받을 수 없었다. default 메서드라 기존 구현은 그대로 컴파일된다.

---

## 4. 실전 예제

### 4.1 제거된 API 치환

```java
// ─── exchange() ───────────────────────────────────────────────
// ❌ 7.0에서 컴파일 에러. 게다가 404/500에서 본문을 소비하지 않아 커넥션이 누수된다
webClient.get().uri("/orders/{id}", id).exchange()
        .flatMap(response -> response.bodyToMono(Order.class));

// ✅ (a) 상태 분기가 필요 없으면 retrieve()
webClient.get().uri("/orders/{id}", id).retrieve().bodyToMono(Order.class);

// ✅ (b) 응답 헤더/상태를 보고 분기해야 하면 exchangeToMono()
//     핸들러가 반환하는 Mono가 완료되면 Spring이 남은 본문을 자동 해제한다
webClient.get().uri("/orders/{id}", id)
        .exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(Order.class);
            }
            if (response.statusCode().value() == 404) {
                return response.releaseBody().then(Mono.empty());
            }
            return response.createError();   // WebClientResponseException으로 변환
        });

// ─── syncBody() / context() ───────────────────────────────────
// ❌ .syncBody(command)                    ✅ .bodyValue(command)
// ❌ .context(ctx -> ctx.put("t", id))     ✅ 결과에 .contextWrite(ctx -> ctx.put("t", id))

// ─── basicAuthentication() 무인자 + Credentials (7.0에서 전부 제거) ───
// ❌ .filter(ExchangeFilterFunctions.basicAuthentication())
//    .attributes(ExchangeFilterFunctions.Credentials.basicAuthenticationCredentials(u, p))

// ✅ 고정 자격증명이면 7.0.8+ 오버로드 (UTF-8 지원)
WebClient client = builder
        .filter(ExchangeFilterFunctions.basicAuthentication(
                props.getUsername(), props.getPassword(), StandardCharsets.UTF_8))
        .build();

// ✅ 요청별 자격증명이 필요하면 요청 속성 + 필터로 직접 구현한다
ExchangeFilterFunction perRequestBasicAuth = (request, next) ->
        request.attribute(CREDENTIALS_ATTRIBUTE)
                .map(BasicCredentials.class::cast)
                .map(cred -> next.exchange(ClientRequest.from(request)
                        .headers(headers -> headers.setBasicAuth(HttpHeaders.encodeBasicAuth(
                                cred.username(), cred.password(), StandardCharsets.UTF_8)))
                        .build()))
                .orElseGet(() -> next.exchange(request));

// ─── ReactorResourceFactory (6.2에서 미리 바꿔 두면 무사통과) ──────
// ❌ import org.springframework.http.client.reactive.ReactorResourceFactory;
// ✅ import org.springframework.http.client.ReactorResourceFactory;
```

### 4.2 `@ImportHttpServices` 로 WebClient 그룹 등록 (7.0 전용)

```java
package com.example.client;

// java.time.Duration, io.netty.channel.ChannelOption
// reactor.netty.http.client.HttpClient, reactor.netty.resources.ConnectionProvider
// org.springframework.context.annotation.{Bean,Configuration}
// org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.support.NotFoundWebClientAdapterDecorator;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * @HttpExchange 인터페이스를 그룹 단위로 등록한다. (Spring Framework 7.0+)
 * clientType을 명시하지 않으면 RestClient(블로킹)가 쓰인다 — WebFlux에서는 치명적이다.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "order", clientType = HttpServiceGroup.ClientType.WEB_CLIENT,
        types = {OrderApi.class, OrderItemApi.class})
@ImportHttpServices(group = "payment", clientType = HttpServiceGroup.ClientType.WEB_CLIENT,
        basePackageClasses = PaymentApi.class)
public class HttpServicesConfig {

    /**
     * WebClientHttpServiceGroupAdapter는 createClientBuilder()에서 WebClient.builder()를
     * 맨 처음부터 만든다. 애플리케이션의 커스터마이즈는 여기서 넣어야 한다.
     */
    @Bean
    WebClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(
            ConnectionProvider provider, ClientProperties props) {

        return groups -> {
            groups.filterByName("order").forEachClient((group, builder) ->
                    builder.baseUrl(props.getOrderBaseUrl())
                            .clientConnector(connector(provider, props.getOrderResponseTimeout())));

            groups.filterByName("payment").forEachClient((group, builder) ->
                    builder.baseUrl(props.getPaymentBaseUrl())
                            .clientConnector(connector(provider, props.getPaymentResponseTimeout())));

            // 404를 빈 Mono로 — 어댑터 레벨에서 그룹 전체에 적용
            groups.filterByName("order").forEachProxyFactory((group, factoryBuilder) ->
                    factoryBuilder.exchangeAdapterDecorator(NotFoundWebClientAdapterDecorator::new));
        };
    }

    private ReactorClientHttpConnector connector(ConnectionProvider provider, Duration responseTimeout) {
        return new ReactorClientHttpConnector(HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
                .responseTimeout(responseTimeout)
                .metrics(true, uri -> uri));
    }

    @Bean
    ConnectionProvider sharedConnectionProvider(ClientProperties props) {
        return ConnectionProvider.builder("http-services")
                .maxConnections(props.getMaxConnections())
                .maxIdleTime(props.getMaxIdleTime())
                .maxLifeTime(props.getMaxLifeTime())
                // reactor-netty 1.3.4+ — 동시 만료를 분산시킨다 (§3.10)
                .maxLifeTimeVariance(props.getMaxLifeTimeVariancePercent())
                .evictInBackground(Duration.ofSeconds(30))
                .metrics(true)
                .build();
    }
}
```

인터페이스는 6.2와 동일하게 `@HttpExchange` 계열 애노테이션만 쓴다. 소비 측은 프록시 빈을 직접 주입하거나(`OrderApi` 를 생성자 파라미터로), 같은 타입이 여러 그룹에 있을 때는 `registry.getClient("order", OrderApi.class)` 로 그룹명을 지정해 꺼낸다.

### 4.3 API 버저닝 (7.0 전용)

```java
@Bean
WebClient catalogWebClient(WebClient.Builder builder, ClientProperties props) {
    return builder
            .baseUrl(props.getCatalogBaseUrl())
            // 버전을 어디에 넣을지 — useHeader / useQueryParam / usePathSegment / builder()
            .apiVersionInserter(ApiVersionInserter.useHeader(props.getVersionHeaderName()))
            // 요청별로 지정하지 않으면 이 값이 쓰인다
            .defaultApiVersion(props.getDefaultApiVersion())
            .build();
}
```

```java
// 요청별 오버라이드
catalogWebClient.get().uri("/products/{id}", id)
        .apiVersion("2024-11-01").retrieve().bodyToMono(Product.class);

// 버전 삽입을 명시적으로 거부 (defaultApiVersion도 적용되지 않는다)
catalogWebClient.get().uri("/health").apiVersion(null).retrieve().toBodilessEntity();
```

```yaml
app:
  client:
    catalog:
      base-url: ${CATALOG_API_BASE_URL}
      version-header-name: X-API-Version
      default-api-version: "2024-11-01"
```

### 4.4 Jackson 2 → 3 전환

```groovy
dependencies {
    // 제거: implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("tools.jackson.core:jackson-databind")
}
```

Jackson 2를 당장 유지해야 하면 **아무 것도 하지 않아도 7.0.9가 폴백해 준다**(§3.2). 단 코덱 접근자는 새 이름을 쓴다.

```java
// tools.jackson.databind.json.JsonMapper
// org.springframework.http.codec.json.{JacksonJsonDecoder,JacksonJsonEncoder}
@Bean
WebClient.Builder jacksonConfiguredBuilder(JsonMapper jsonMapper, ClientProperties props) {
    ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs()
                        .maxInMemorySize((int) props.getMaxInMemorySize().toBytes());
                // 7.0: jackson2JsonDecoder(...) 는 deprecated — 새 이름을 쓴다
                configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
                configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
            })
            .build();
    return WebClient.builder().exchangeStrategies(strategies);
}
```

전환 체크리스트:

1. `com.fasterxml.jackson.*` → `tools.jackson.*` (패키지가 완전히 다르다)
2. `ObjectMapper` → `JsonMapper` (Jackson 3의 기본 진입점)
3. `Jackson2JsonDecoder`/`Encoder` 직접 생성 코드를 `JacksonJsonDecoder`/`Encoder` 로
4. `CodecConfigurer` 호출을 `jackson2*` → `jacksonJson*` 로
5. **두 Jackson이 공존하면 `JACKSON_PRESENT` 가 우선**이므로, 의존성을 추가하는 순간 기본 코덱이 바뀐다. 커스텀 매퍼 설정이 새 코덱에도 적용되는지 반드시 확인
6. 직렬화 스냅샷 테스트를 전부 돌려라 — Jackson 3는 일부 기본값이 다르다

### 4.5 단계적 마이그레이션 순서

```
6.2.19에서 미리 할 일 (업그레이드 전, 안전함)
──────────────────────────────────────────────────────────
1. exchange() → retrieve() / exchangeToMono()
2. syncBody() → bodyValue()
3. context() → .contextWrite()
4. basicAuthentication() 무인자 + Credentials 제거
5. http.client.reactive.ReactorResourceFactory
      → http.client.ReactorResourceFactory
6. ReactorNetty2* 커넥터 사용 중단
7. ClientRequestObservationContext() 무인자 생성자 제거(테스트)
   → 여기까지 하면 deprecation 경고가 0이 되고, 7.0 업그레이드가
      "의존성 버전만 바꾸는 일"이 된다

7.0.9로 올린 뒤
──────────────────────────────────────────────────────────
8. reactor.util.annotation.Nullable → org.jspecify.annotations.Nullable
   @NonNullApi → @NullMarked
9. UnprocessableEntity → UnprocessableContent (쓰고 있다면)
10. Jackson 3 전환 (별도 PR로 분리 — 직렬화 회귀 위험)
11. (선택) @ImportHttpServices / apiVersionInserter / maxLifeTimeVariance 도입
```

8번까지는 기계적이다. 10번은 동작이 바뀔 수 있으므로 반드시 별도 PR로 분리하고 계약 테스트를 돌려야 한다.

---

## 5. 정리

### 5.1 변경 요약

| 항목 | 6.2.19 | 7.0.9 | 대응 |
|---|---|---|---|
| `exchange()` | `@Deprecated` (since 5.3, 누수 위험) | 제거 (내부 `private` 로만) | `retrieve()` / `exchangeToMono()` |
| `syncBody()` | `@Deprecated` (since 5.2) | 제거 | `bodyValue()` |
| `context(Function)` | `@Deprecated` (since 5.3.2) | 제거 | `.contextWrite()` |
| `basicAuthentication()` 무인자 + `Credentials` + 속성 상수 | `@Deprecated` (since 5.1) | 제거 | `HttpHeaders.setBasicAuth` 또는 커스텀 필터 |
| `basicAuthentication(String,String,Charset)` | 없음 | **`@since 7.0.8` — 7.0.9에 있음** | UTF-8 자격증명에 사용 |
| `ClientRequestObservationContext()` 무인자 | `@Deprecated(6.1.2, forRemoval)` | 제거 | `(ClientRequest.Builder)` 생성자 |
| `UnprocessableEntity` | 정상 | `@Deprecated(since="7.0")` | `UnprocessableContent` |
| `Jackson2JsonDecoder`/`Encoder` | 정상 (기본 코덱) | `@Deprecated(7.0, forRemoval)` | `JacksonJsonDecoder`/`Encoder` (`@since 7.0`) |
| `CodecConfigurer.jackson2Json*` | 추상 메서드 | `default` + `@Deprecated(7.0, forRemoval)` | `jacksonJson*` |
| Jackson 감지 클래스 | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` 우선, Jackson 2 폴백 | 의존성 교체 시 코덱이 조용히 바뀜 — 확인 필수 |
| `web.service.registry` 패키지 | **없음** | 전체 `@since 7.0` | `@ImportHttpServices` + `HttpServiceProxyRegistry` |
| `WebClientHttpServiceGroupAdapter` | 없음 | `@since 7.0` | `clientType = WEB_CLIENT` 를 **반드시 명시** (기본은 RestClient) |
| `NotFoundWebClientAdapterDecorator` | 없음 | `@since 7.0` | 404 → 빈 `Mono` |
| `defaultApiVersion` / `apiVersionInserter` / `apiVersion` | 없음 | 모두 `@since 7.0` | `ApiVersionInserter` 는 `org.springframework.web.client` 에 |
| `ReactorNetty2*` 커넥터 | 존재 | 제거 | `ReactorClientHttpConnector` |
| `http.client.reactive.ReactorResourceFactory` | `@Deprecated(since="6.1")` | 제거 | `http.client.ReactorResourceFactory` |
| nullability 어노테이션 | `org.springframework.lang.Nullable` | `org.jspecify.annotations.Nullable` | 컴파일은 안 깨짐 |
| **Observation 이름·태그** | — | **변화 없음** | 대시보드 수정 불필요 |
| **`ExchangeFilterFunction`/`ExchangeFunction`** | — | **변화 없음** | 필터 코드 그대로 |

| Reactor Core | 3.7.19 | 3.8.7 |
|---|---|---|
| `reactor.util.annotation.Nullable`/`NonNull`/`NonNullApi` | 정상 | 전부 `@Deprecated` → jspecify로 |
| jspecify 사용 파일 (main) | 0개 | 393개 |
| `reactor.util.retry` / `Schedulers` 공개 API | — | **변화 없음** |
| `@since 3.8` 신규 공개 API | — | **main 소스에 없음** |

| reactor-netty | 1.2.18 | 1.3.7 | `@since` |
|---|---|---|---|
| `evictInBackground(Duration, Scheduler)` | 없음 | 있음 | 1.3.2 |
| `maxLifeTimeVariance(double)` | 없음 | 있음 | 1.3.4 |
| `MeterRegistrar.recordPendingAcquireSuccess/Failure` | 없음 | default 메서드 | 1.3.7 |
| `httpAuthentication` / `httpAuthenticationWhen` | 없음 | 있음 (401 자동 재시도) | 1.3.1 |
| `Http2AllocationStrategy.strictConnectionReuse` / `streamBatchSize` | 없음 | 있음 | 1.3.3 / 1.3.4 |
| WebSocket 클라이언트 메트릭 | 없음 | 있음 | 1.3.7 |
| `HttpClientMetricsRecorder` / `HttpClient` deprecation | — | **변화 없음** | — |

### 5.2 소스에서 확인되지 않아 문서에 넣지 않은 항목

| 항목 | 이유 |
|---|---|
| Spring Boot 4의 자동 설정·프로퍼티 변경 | Boot 소스 워크트리가 없어 확인 불가. 이 문서는 Framework 레벨만 다룬다 |
| Boot 4가 `@ImportHttpServices` 그룹에 자동으로 꽂아 주는 설정(관측·로드밸런서 등) | 같은 이유. §4.2는 Framework만으로 설정하는 형태로 썼다 |
| `boundedElastic` 가상 스레드 옵션 | **델타가 아니다.** `Schedulers` Javadoc에 "Since 3.6.0"으로 적혀 있고 3.7.19에도 `reactor.schedulers.defaultBoundedElasticOnVirtualThreads` 시스템 프로퍼티가 이미 있다 |
| Java 베이스라인 상향 | 7.0.9 빌드의 `DEFAULT_RELEASE_VERSION` 이 17로 남아 있어(툴체인은 25) 바이트코드 타깃 변경을 단정할 수 없다 |
| `CodecConfigurer` 의 `decoder`/`encoder`/`reader`/`writer`/`withDefaultCodecConfig`/`isEnableLoggingRequestDetails` 제거 여부 | 시그니처 grep에서만 차이가 보이고 Javadoc 확인이 끝나지 않았다. Jackson 관련 변경만 기재 |
| reactor-netty의 Netty 버전 상향 | `nettyDefaultVersion` 이 `gradle.properties` 에 없어 확인 불가 |

---

## 관련 문서
- 선행: [WebClient 테스트](11-testing-webclient.md)
- 선행: [retrieve vs exchange](03-retrieve-vs-exchange.md)
- 선행: [코덱과 DataBuffer](05-codecs-and-databuffer.md)
- 선행: [커넥션 재사용의 함정](08-connection-reuse-pitfalls.md)
- 선행: [재시도와 복원력](09-resilience-integration.md)
- 선행: [관측성](10-observability.md)
- 처음으로: [WebClient 빌더와 커넥터](01-webclient-builder-connectors.md)

---
*참고: Spring Framework 6.2.19 → 7.0.9, Reactor Core 3.7.19 → 3.8.7, reactor-netty 1.2.18 → 1.3.7 비교*
