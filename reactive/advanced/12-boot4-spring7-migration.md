# Spring Boot 4 / Framework 7 마이그레이션 — WebClient 관점

Spring Framework 6.2.19에서 7.0.9로 올릴 때 WebClient 사용 코드가 실제로 깨지는 지점과, 새로 생긴 선택지를 양쪽 소스를 직접 비교해 정리했다. **두 워크트리에서 확인된 항목만** 담았고, 확인되지 않은 항목은 §5에 따로 적었다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

변경은 네 묶음으로 나뉜다.

```
┌─ A. 제거 (컴파일 에러) ─────────────────────────────────────┐
│   exchange() / syncBody() / context()                      │
│   basicAuthentication() 무인자 + Credentials               │
│   ReactorNetty2ClientHttpConnector 계열                     │
│   http.client.reactive.ReactorResourceFactory              │
│   ClientRequestObservationContext() 무인자 생성자            │
└────────────────────────────────────────────────────────────┘
┌─ B. deprecated (경고, 아직 동작) ──────────────────────────┐
│   Jackson2JsonDecoder / Jackson2JsonEncoder                │
│   CodecConfigurer.jackson2JsonDecoder / …Encoder           │
│   WebClientResponseException.UnprocessableEntity           │
│   (Reactor) reactor.util.annotation.Nullable / NonNull…    │
└────────────────────────────────────────────────────────────┘
┌─ C. 신규 기능 (쓸지 선택) ─────────────────────────────────┐
│   @ImportHttpServices + HttpServiceProxyRegistry           │
│   defaultApiVersion / apiVersionInserter / apiVersion       │
│   NotFoundWebClientAdapterDecorator                        │
│   basicAuthentication(String, String, Charset)             │
│   JacksonJsonDecoder / JacksonJsonEncoder (Jackson 3)      │
│   (netty) maxLifeTimeVariance / httpAuthentication / …     │
└────────────────────────────────────────────────────────────┘
┌─ D. 변화 없음 (안심) ──────────────────────────────────────┐
│   Observation 이름·태그 전체                                │
│   ExchangeFilterFunction / ExchangeFunction 시그니처        │
│   ClientRequest / ClientResponse 빌더                       │
│   Reactor retry·Schedulers 공개 API                         │
│   HttpClientMetricsRecorder 인터페이스                      │
└────────────────────────────────────────────────────────────┘
```

버전 좌표:

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

같은 Javadoc에 누수 경고가 따로 있다 (`v6.2.19 WebClient.java:626-631`):

> **NOTE:** Unlike {@link #retrieve()}, when using {@code exchange()}, it is the responsibility of the application to consume any response content regardless of the scenario (success, error, unexpected data, etc). Not doing so can cause a memory leak.

7년 가까이 예고된 제거이므로, 6.2에 머물러 있는 동안 전부 치워 두는 게 정석이다. 치우는 방법은 `exchange()` 가 왜 필요했는지에 따라 갈린다([`03-retrieve-vs-exchange.md`](03-retrieve-vs-exchange.md) 참고).

### 2.2 Jackson 3는 **다른 좌표의 다른 패키지**다

클래스 이름만 바뀐 게 아니다. 감지하는 클래스 자체가 다르다.

```java
// v6.2.19 BaseDefaultCodecs.java:106
jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
                ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
```
```java
// v7.0.9 BaseDefaultCodecs.java:130-132
JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
                ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
```

Jackson 3의 루트 패키지는 **`tools.jackson`** 이다. 즉 Jackson 2와 **클래스패스에 공존할 수 있다**. 7.0.9는 둘 다 지원하고 Jackson 3를 우선한다(§3.2). 그래서 마이그레이션이 한 번에 끊기지 않고, **"의존성을 바꾸는 순간 기본 코덱이 조용히 교체된다"**는 다른 종류의 위험이 생긴다. 커스텀 `ObjectMapper` 를 주입해 쓰고 있었다면 그 설정이 적용되지 않는 상태로 돌아갈 수 있다.

### 2.3 `@ImportHttpServices` 는 WebClient를 "그룹"으로 다룬다

6.2에서 `@HttpExchange` 인터페이스 클라이언트를 쓰려면 프록시를 수동으로 만들어야 했다.

```java
// 6.2 방식 — 서비스마다 빈 하나씩 손으로
HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient)).build();
OrderApi orderApi = factory.createClient(OrderApi.class);
```

7.0은 그 등록을 선언으로 바꾼다. `org.springframework.web.service.registry` 패키지가 **6.2.19에는 존재하지 않는다** — `web/service/` 아래에 `annotation`, `invoker` 만 있다. 7.0.9에서 `registry` 가 추가되고 그 안의 모든 타입이 `@since 7.0` 이다.

```java
// 7.0 방식 — 그룹 단위 선언
@Configuration
@ImportHttpServices(group = "order", clientType = ClientType.WEB_CLIENT,
        types = {OrderApi.class, PaymentApi.class})
class HttpServicesConfig { }
```

이득은 **그룹별 공통 설정**이다. 같은 그룹의 모든 인터페이스가 하나의 `WebClient.Builder` 를 공유하므로, baseUrl·타임아웃·필터·관측 설정을 한곳에서 준다. 6.2에서는 서비스마다 반복해야 했다.

---

## 3. 내부 구현 분석 (How)

### 3.1 제거된 WebClient API

`WebClient.java` 의 공개 시그니처를 양쪽에서 비교한 결과, **제거 3건 / 추가 3건**이다.

| 항목 | 6.2.19 상태 | 7.0.9 상태 | 대응 |
|---|---|---|---|
| `RequestHeadersSpec.exchange()` | `@Deprecated` 존재 (`WebClient.java:644-645`) | **제거**. 내부에만 `private Mono<ClientResponse> exchange()` 로 남음 (`DefaultWebClient.java:442`) | `retrieve()` 또는 `exchangeToMono(Function)` / `exchangeToFlux(Function)` |
| `RequestBodySpec.syncBody(Object)` | `@Deprecated` 존재 (`WebClient.java:798-799`)<br/>원문: *"deprecated as of Spring Framework 5.2 in favor of `bodyValue(Object)`"* | **제거** | `bodyValue(Object)` |
| `RequestHeadersSpec.context(Function<Context,Context>)` | `@Deprecated` 존재 (`WebClient.java:512-513`)<br/>원문: *"deprecated in 5.3.2 to be removed soon after; this method cannot provide context to downstream (nested or subsequent) requests and is of limited value"* | **제거** | 호출 결과에 `.contextWrite(...)` |
| `Builder.defaultApiVersion(Object)` | 없음 | 추가, `@since 7.0` (`WebClient.java:262`) | — |
| `Builder.apiVersionInserter(@Nullable ApiVersionInserter)` | 없음 | 추가, `@since 7.0` (`WebClient.java:274`) | — |
| `RequestHeadersSpec.apiVersion(@Nullable Object)` | 없음 | 추가, `@since 7.0` (`WebClient.java:515`) | — |

`ApiVersionInserter` 는 webflux가 아니라 **`org.springframework.web.client`** (spring-web)에 있다 (`v7.0.9 spring-web/.../web/client/ApiVersionInserter.java:43`) — `RestClient` 와 공유하는 추상이다. 6.2.19에는 이 타입이 없다. 정적 팩토리로 삽입 위치를 고른다:

| 팩토리 | 줄 | 버전이 들어가는 곳 |
|---|---|---|
| `useHeader(@Nullable String header)` | `:70` | 요청 헤더 |
| `useQueryParam(@Nullable String queryParam)` | `:78` | 쿼리 파라미터 |
| `usePathSegment(@Nullable Integer pathSegmentIndex)` | `:94` | 경로 세그먼트 |
| `builder()` | `:101` | 여러 위치 조합 + 포맷터 지정 |

`apiVersion(Object)` 의 Javadoc이 동작 규칙을 명시한다 (`v7.0.9 WebClient.java:501-514`):

> The version is inserted into the request through the {@link Builder#apiVersionInserter(ApiVersionInserter) configured} {@code ApiVersionInserter}. … If no version is set, the {@link Builder#defaultApiVersion(Object) defaultApiVersion} is used, if configured. If {@code null} is passed, then an API version is not inserted irrespective of default version settings.

즉 `apiVersion(null)` 은 "기본 버전도 넣지 말라"는 명시적 거부다.

### 3.2 JSON 코덱 이동

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| 기본 JSON 디코더 | `Jackson2JsonDecoder` (`http/codec/json/`) | `JacksonJsonDecoder` `@since 7.0` |
| 기본 JSON 인코더 | `Jackson2JsonEncoder` | `JacksonJsonEncoder` `@since 7.0` |
| 구 클래스 상태 | 정상 (deprecation 없음) | `@Deprecated(since = "7.0", forRemoval = true)` |
| 베이스 클래스 | `AbstractJackson2Decoder` / `…Encoder` | `AbstractJacksonDecoder<JsonMapper>` / `…Encoder` (`http/codec/` 루트로 승격) |
| 추가 코덱 | — | `GsonDecoder`/`GsonEncoder` `@since 7.0` |

`Jackson2JsonDecoder` 의 7.0.9 deprecation 원문 (`v7.0.9 Jackson2JsonDecoder.java:45-50`):

```java
 * @since 5.0
 * @see Jackson2JsonEncoder
 * @deprecated since 7.0 in favor of {@link AbstractJacksonDecoder}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2JsonDecoder extends AbstractJackson2Decoder {
```

`JacksonJsonDecoder` 의 선언 (`v7.0.9 JacksonJsonDecoder.java:41-49`):

```java
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/FasterXML/jackson">Jackson 3.x</a>
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractJacksonDecoder<JsonMapper> {
```

선택 로직은 **Jackson 3 우선, 없으면 Jackson 2 폴백**이다 (`v7.0.9 BaseDefaultCodecs.getJacksonJsonDecoder()`, `BaseDefaultCodecs.java:871-886`):

```java
protected Decoder<?> getJacksonJsonDecoder() {
    if (this.jacksonJsonDecoder == null) {
        if (JACKSON_PRESENT) {
            this.jacksonJsonDecoder = new JacksonJsonDecoder();
        }
        else if (JACKSON_2_PRESENT) {
            this.jacksonJsonDecoder = new Jackson2JsonDecoder();
        }
        else {
            throw new IllegalStateException("Jackson not present");
        }
    }
    return this.jacksonJsonDecoder;
}
```

구 접근자는 새 것으로 위임하도록 남았다 (`v7.0.9 BaseDefaultCodecs.java:889-894`):

```java
 * @deprecated in favor of {@link #getJacksonJsonDecoder()}
 */
@Deprecated
protected Decoder<?> getJackson2JsonDecoder() {
    return getJacksonJsonDecoder();
}
```

`CodecConfigurer.DefaultCodecs` 쪽 변화:

| 메서드 | 6.2.19 | 7.0.9 |
|---|---|---|
| `jacksonJsonDecoder(Decoder<?>)` | 없음 | 추가, `@since 7.0` (`CodecConfigurer.java:121`) |
| `jacksonJsonEncoder(Encoder<?>)` | 없음 | 추가, `@since 7.0` |
| `jackson2JsonDecoder(Decoder<?>)` | 추상 메서드 | `default` + `@Deprecated(since="7.0", forRemoval=true)` (`CodecConfigurer.java:129-132`)<br/>원문: *"deprecated in favor of {@link #jacksonJsonDecoder(Decoder)}"* |
| `jackson2JsonEncoder` / `jackson2SmileDecoder` / `jackson2SmileEncoder` | 추상 메서드 | 같은 패턴으로 `default` + deprecated |
| `gsonDecoder` / `gsonEncoder` | 없음 | 추가 |
| `jacksonCborDecoder`/`Encoder`, `jacksonSmileDecoder`/`Encoder`, `jacksonXmlDecoder`/`Encoder` | 없음 | 추가 |

추상 메서드가 `default` 로 내려간 덕분에 `CodecConfigurer.DefaultCodecs` 를 직접 구현한 코드는 깨지지 않는다 — 하지만 호출하는 쪽은 deprecation 경고를 받는다.

### 3.3 `@ImportHttpServices` 와 그룹 어댑터

6.2.19와 7.0.9의 `web/service/` 디렉터리 구성:

```
v6.2.19                          v7.0.9
web/service/                     web/service/
  ├─ annotation/                   ├─ annotation/
  └─ invoker/                      ├─ invoker/
                                   └─ registry/          ← 신규, 전부 @since 7.0
                                        AbstractHttpServiceRegistrar
                                        GroupsMetadata
                                        HttpServiceGroup
                                        HttpServiceGroupAdapter
                                        HttpServiceGroupConfigurer
                                        HttpServiceProxyBeanRegistrationAotProcessor
                                        HttpServiceProxyRegistry
                                        HttpServiceProxyRegistryFactoryBean
                                        ImportHttpServiceRegistrar
                                        ImportHttpServices
```

`@ImportHttpServices` 의 속성 (`v7.0.9 ImportHttpServices.java`):

| 속성 | 기본값 | 의미 |
|---|---|---|
| `value()` / `types()` | `{}` | 등록할 `@HttpExchange` 인터페이스 (서로 `@AliasFor`) |
| `group()` | `HttpServiceGroup.DEFAULT_GROUP_NAME` = `"default"` | 그룹 이름 |
| `basePackageClasses()` | `{}` | 이 클래스들의 패키지에서 인터페이스 자동 탐색 |
| `basePackages()` | `{}` | 패키지명으로 자동 탐색 |
| `clientType()` | `ClientType.UNSPECIFIED` | `REST_CLIENT` / `WEB_CLIENT` / `UNSPECIFIED` |

`@Repeatable(ImportHttpServices.Container.class)` 이고 `@Import(ImportHttpServiceRegistrar.class)` 가 붙어 있다. 즉 `@Configuration` 클래스에 여러 번 붙여 그룹을 여러 개 만들 수 있다.

**중요 — 기본 클라이언트는 `RestClient` 다** (`v7.0.9 ImportHttpServices.java:90-95`):

> By default, this is {@link HttpServiceGroup.ClientType#UNSPECIFIED} in which case {@code RestClient} is used, but this default can be changed via {@link AbstractHttpServiceRegistrar#setDefaultClientType}.

WebClient를 쓰려면 **반드시 `clientType = ClientType.WEB_CLIENT` 를 명시**해야 한다. 안 쓰면 블로킹 `RestClient` 프록시가 만들어지고, WebFlux 이벤트 루프에서 블로킹이 일어난다.

어댑터 탐색 방식은 `spring.factories` 가 아니라 **클래스 이름 문자열 + `ClassUtils.forName`** 이다 (`v7.0.9 HttpServiceProxyRegistryFactoryBean.GroupAdapterInitializer`, `HttpServiceProxyRegistryFactoryBean.java:140-166`):

```java
private static final String WEB_CLIENT_HTTP_SERVICE_GROUP_ADAPTER =
        "org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupAdapter";

static Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> initGroupAdapters() {
    Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> map = new LinkedHashMap<>(2);
    addGroupAdapter(map, HttpServiceGroup.ClientType.REST_CLIENT, REST_CLIENT_HTTP_SERVICE_GROUP_ADAPTER);
    addGroupAdapter(map, HttpServiceGroup.ClientType.WEB_CLIENT, WEB_CLIENT_HTTP_SERVICE_GROUP_ADAPTER);
    return map;
}

private static void addGroupAdapter(..., String className) {
    try {
        Class<?> clazz = ClassUtils.forName(className, HttpServiceGroupAdapter.class.getClassLoader());
        groupAdapters.put(clientType, (HttpServiceGroupAdapter<?>) BeanUtils.instantiateClass(clazz));
    }
    catch (ClassNotFoundException ignored) {
    }
}
```

**`spring-webflux` 가 클래스패스에 없으면 `WEB_CLIENT` 어댑터가 조용히 등록되지 않는다**(`ClassNotFoundException` 을 무시한다). WebFlux 의존성이 빠진 모듈에서 `clientType = WEB_CLIENT` 를 쓰면 런타임 에러가 난다.

어댑터 본문 (`v7.0.9 WebClientHttpServiceGroupAdapter.java:31-47`):

```java
public class WebClientHttpServiceGroupAdapter implements HttpServiceGroupAdapter<WebClient.Builder> {

    @Override
    public WebClient.Builder createClientBuilder() {
        return WebClient.builder();
    }

    @Override
    public Class<? extends HttpServiceGroupConfigurer<WebClient.Builder>> getConfigurerType() {
        return WebClientHttpServiceGroupConfigurer.class;
    }

    @Override
    public HttpExchangeAdapter createExchangeAdapter(WebClient.Builder clientBuilder) {
        return WebClientAdapter.create(clientBuilder.build());
    }
}
```

`WebClient.builder()` 를 **맨 처음부터** 만든다 — 애플리케이션이 커스터마이즈한 `WebClient.Builder` 빈을 쓰지 않는다. 설정은 `WebClientHttpServiceGroupConfigurer` 빈으로 주입해야 한다(§4.2).

그룹별 설정 DSL은 `HttpServiceGroupConfigurer<CB>` 다 (`v7.0.9 HttpServiceGroupConfigurer.java`). WebClient용 하위 타입이 `WebClientHttpServiceGroupConfigurer extends HttpServiceGroupConfigurer<WebClient.Builder>` (`:29`).

| 메서드 | 줄 | 역할 |
|---|---|---|
| `Groups<CB> filterByName(String...)` | `:59` | 그룹 이름으로 범위 좁히기 |
| `Groups<CB> filter(Predicate<HttpServiceGroup>)` | `:64` | 임의 조건으로 좁히기 |
| `void forEachClient(ClientCallback<CB>)` | `:71` | `withClient(group, clientBuilder)` — 빌더 커스터마이즈 |
| `void forEachClient(InitializingClientCallback<CB>)` | `:78` | `initClient(group)` — 빌더를 **직접 제공** |
| `void forEachProxyFactory(ProxyFactoryCallback)` | `:85` | `withProxyFactory(group, HttpServiceProxyFactory.Builder)` |

`forEachProxyFactory` 로 받은 빌더에 `exchangeAdapterDecorator(Function<HttpExchangeAdapter, HttpExchangeAdapter>)` (`v7.0.9 HttpServiceProxyFactory.java:178`) 를 걸면 §3.4의 404 데코레이터를 그룹 단위로 적용할 수 있다.

`HttpServiceProxyRegistry` 의 조회 API (`v7.0.9 HttpServiceProxyRegistry.java`):

```java
<P> P getClient(Class<P> httpServiceType);              // 전체 그룹에서 유일할 때만
<P> P getClient(String groupName, Class<P> httpServiceType);
Set<String> getGroupNames();
Set<Class<?>> getClientTypesInGroup(String groupName);
```

### 3.4 `NotFoundWebClientAdapterDecorator`

6.2.19에 없고, 7.0.9의 `…/function/client/support/` 에 `@since 7.0` 으로 추가됐다. 404를 예외가 아니라 "없음"으로 바꿔 주는 데코레이터다.

```java
// v7.0.9 NotFoundWebClientAdapterDecorator.java:40
public class NotFoundWebClientAdapterDecorator extends ReactorHttpExchangeAdapterDecorator {
```

| 메서드 | 404일 때 결과 | 줄 |
|---|---|---|
| `exchangeForBody(...)` | `null` | `:48-55` |
| `exchangeForBodilessEntity(...)` | `ResponseEntity.notFound().build()` | `:57-65` |
| `exchangeForEntity(...)` | `ResponseEntity.notFound().build()` | `:67-75` |
| `exchangeForBodyMono(...)` | **빈 `Mono`** | `:77-80` |
| `exchangeForBodyFlux(...)` | 빈 `Flux` | `:82-86` |

잡는 예외는 `WebClientResponseException.NotFound` 하나다. 리액티브 경로의 구현 (`:77-80`):

```java
@Override
public <T> Mono<T> exchangeForBodyMono(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return super.exchangeForBodyMono(values, bodyType).onErrorResume(
            WebClientResponseException.NotFound.class, ex -> Mono.empty());
}
```

6.2에서 같은 효과를 내려면 각 호출마다 `.onStatus(NOT_FOUND::equals, r -> r.releaseBody().then(Mono.empty()))` 를 붙이거나 `onErrorResume` 을 써야 했다. 7.0에서는 어댑터 레벨에서 한 번에 끝난다.

### 3.5 `basicAuthentication` 오버로드

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `basicAuthentication(String, String)` | 존재 (`ExchangeFilterFunctions.java:91`) | 존재 (`ExchangeFilterFunctions.java:91`), ISO-8859-1 사용 명시 |
| `basicAuthentication(String, String, Charset)` | **없음** | 존재, **`@since 7.0.8`** (`ExchangeFilterFunctions.java:104-109`) |
| `basicAuthentication()` (무인자) | 존재, `@Deprecated` (`:112-113`)<br/>원문: *"deprecated as of Spring 5.1 in favor of using `HttpHeaders#setBasicAuth(String, String)` while building the request"* | **제거** |
| `ExchangeFilterFunctions.Credentials` | 존재, `@Deprecated` (`:133`) | **제거** |
| `BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE` | 존재 (`:47-48`) | **제거** |

7.0.9의 새 오버로드 (`v7.0.9 ExchangeFilterFunctions.java:104-115`):

```java
 * @param charset the charset to use to convert the credentials into an octet
 * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
 * @return the filter to add authentication headers with
 * @since 7.0.8
 * ...
public static ExchangeFilterFunction basicAuthentication(String username, String password, @Nullable Charset charset) {
    String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
    return (request, next) ->
            next.exchange(ClientRequest.from(request)
                    .headers(headers -> headers.setBasicAuth(encodedCredentials))
                    .build());
}
```

**`@since 7.0.8` 이므로 7.0.9에 들어 있다** — 워크트리에서 확인했다. 7.0.0~7.0.7을 쓰면 없다. UTF-8 비밀번호가 필요하면 이 오버로드가 유일한 공식 경로다(그 전에는 직접 `HttpHeaders.encodeBasicAuth(u, p, UTF_8)` 를 호출해야 했다).

또 하나: 새 오버로드는 **자격증명 인코딩을 필터 생성 시점에 한 번** 수행하고 람다는 결과 문자열만 쓴다. 요청마다 Base64 인코딩이 돌지 않는다.

### 3.6 예외 클래스 변화

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `WebClientResponseException.UnprocessableEntity` | 정상 (`:487`) | `@Deprecated(since = "7.0")` (`:491`)<br/>원문: *"deprecated since 7.0 in favor of {@link UnprocessableContent}"* |
| `WebClientResponseException.UnprocessableContent` | 없음 | 신규 — `HttpStatus.UNPROCESSABLE_CONTENT` 기반 |

HTTP 422의 공식 reason phrase가 "Unprocessable Entity"에서 "Unprocessable Content"로 바뀐 것(RFC 9110)을 따라간 이름 변경이다. `instanceof WebClientResponseException.UnprocessableEntity` 로 분기하는 코드는 경고가 뜨므로 `UnprocessableContent` 로 바꾸되, **상태코드로 분기하던 코드는 영향이 없다**.

### 3.7 커넥터와 리소스 팩토리

`spring-web` 의 `http/client/reactive/` 디렉터리 비교:

| 클래스 | 6.2.19 | 7.0.9 |
|---|---|---|
| `ReactorNetty2ClientHttpConnector` / `…Request` / `…Response` / `ReactorNetty2ResourceFactory` | 존재 (테스트에서 `@SuppressWarnings("removal")` 로 사용) | **제거** |
| `http.client.reactive.ReactorResourceFactory` | 존재, `@Deprecated(since = "6.1")` (`:43-45`)<br/>원문: *"deprecated since 6.1 due to a package change; use {@link org.springframework.http.client.ReactorResourceFactory} instead"* | **제거** |
| `http.client.ReactorResourceFactory` | 존재, `@since 6.1` | 존재 (유일한 위치) |
| `JdkResponseCookieParser` | 없음 | 신규 |
| `ReactorClientHttpConnector`, `JdkClientHttpConnector`, `JettyClientHttpConnector`, `HttpComponentsClientHttpConnector` | 존재 | 존재 (변화 없음) |

`ReactorNetty2*` 는 Netty 5 프리뷰를 위한 실험 코드였고 Netty 5가 중단되면서 함께 사라졌다. Reactor Netty 기반 커넥터는 `ReactorClientHttpConnector` 하나로 정리됐다.

### 3.8 Observation은 바뀌지 않았다

양쪽 `DefaultClientRequestObservationConvention.java` 의 `KeyValue.of(...)` 호출을 전부 비교한 결과 **차이가 없다**. 즉:

- 관측 이름 `http.client.requests` 동일
- low cardinality 태그 `method`, `uri`, `status`, `client.name`, `exception`, `outcome` 동일
- high cardinality 태그 `http.url` 동일
- `IO_ERROR` / `CLIENT_ERROR` / `SUCCESS` / `UNKNOWN` 값 동일

**대시보드와 알림 쿼리를 손댈 필요가 없다.** ([`10-observability.md`](10-observability.md))

단 하나 깨지는 것: `ClientRequestObservationContext` 의 무인자 생성자.

| 항목 | 6.2.19 | 7.0.9 |
|---|---|---|
| `ClientRequestObservationContext()` | 존재, `@Deprecated(since = "6.1.2", forRemoval = true)`<br/>원문: *"deprecated as of 6.1.2, in favor of {@link #ClientRequestObservationContext(ClientRequest.Builder)}"* | **제거** |
| `ClientRequestObservationContext(ClientRequest.Builder)` | 존재, `@since 6.1.2` | 존재 (유일) |
| nullability 어노테이션 | `org.springframework.lang.Nullable` | **`org.jspecify.annotations.Nullable`** (`:20`) |

직접 컨텍스트를 만드는 테스트 코드만 영향받는다.

### 3.9 Spring 7의 jspecify 전환

7.0.9의 webflux 클라이언트 클래스가 jspecify를 쓴다 — 예: `ClientRequestObservationContext.java:20`, `NotFoundWebClientAdapterDecorator.java:19`:

```java
import org.jspecify.annotations.Nullable;
```

6.2.19는 `org.springframework.lang.Nullable` 이다. 선언 위치가 다른 것도 주목할 점이다 — jspecify는 **타입 사용 어노테이션**이라 반환 타입 앞에 붙는다:

```java
// 6.2.19 스타일
@Nullable
public String getUriTemplate() { ... }

// 7.0.9 스타일
public <T> @Nullable T exchangeForBody(...) { ... }
```

Kotlin에서 호출할 때 플랫폼 타입 추론이 더 정확해진다. Java 코드에서는 IDE/정적분석기 경고 양상이 달라질 수 있지만 **컴파일은 깨지지 않는다**.

### 3.10 Reactor Core 3.7.19 → 3.8.7

| 항목 | 3.7.19 | 3.8.7 |
|---|---|---|
| jspecify 사용 파일 수 (main) | **0개** | **393개** |
| `reactor.util.annotation.Nullable` | 정상 | `@Deprecated` — 원문: *"Use {@link org.jspecify.annotations.Nullable} instead. To be removed in …"* (`:49, :57`) |
| `reactor.util.annotation.NonNullApi` | 정상 | `@Deprecated` — 원문: *"Use {@link org.jspecify.annotations.NullMarked} instead. To be removed …"* (`:45, :53`) |
| `reactor.util.annotation.NonNull` | 정상 | `@Deprecated` |
| `reactor.util.retry` 공개 시그니처 | — | **변화 없음** (`RetryBackoffSpec`/`RetrySpec` 전체 메서드 diff 결과 동일) |
| `Schedulers` 공개 정적 메서드 | — | **변화 없음** |
| `@since 3.8` 로 표시된 새 공개 API | — | **main 소스에 없음** |

즉 WebClient 사용자 입장에서 Reactor 3.8.7의 유일한 가시적 변화는 **null 어노테이션의 jspecify 전환**이다. `reactor.util.annotation.Nullable` 을 import한 애플리케이션 코드는 deprecation 경고를 받으므로 `org.jspecify.annotations.Nullable` 로 바꾼다. 패키지 수준 `@NonNullApi` 는 `@NullMarked` 로 교체한다.

### 3.11 reactor-netty 1.2.18 → 1.3.7

| 항목 | 1.2.18 | 1.3.7 | `@since` |
|---|---|---|---|
| `ConnectionProvider.Builder.evictInBackground(Duration)` | 존재 (`:762`) | 존재 (`:796`) | — |
| `ConnectionProvider.Builder.evictInBackground(Duration, Scheduler)` | **없음** | 존재 (`:817`) | **1.3.2** |
| `ConnectionProvider.Builder.maxLifeTimeVariance(double)` | **없음** | 존재 (`:684`) | **1.3.4** |
| `ConnectionProvider.MeterRegistrar.recordPendingAcquireSuccess(...)` | **없음** | default 메서드 | **1.3.7** |
| `ConnectionProvider.MeterRegistrar.recordPendingAcquireFailure(...)` | **없음** | default 메서드 | **1.3.7** |
| `HttpClient.httpAuthentication(BiPredicate, BiConsumer[, int])` | **없음** | 존재 (`:1789`, `:1843`) | **1.3.1** |
| `HttpClient.httpAuthenticationWhen(BiPredicate, BiFunction[, int])` | **없음** | 존재 | **1.3.1** |
| `HttpClientAuthenticationException` | **없음** | 존재 | **1.3.1** |
| `Http2AllocationStrategy.Builder.strictConnectionReuse(boolean)` | **없음** | 존재 (`:89`) | **1.3.3** |
| `Http2AllocationStrategy.Builder.streamBatchSize(int)` | **없음** | 존재 (`:111`) | **1.3.4** |
| WebSocket 클라이언트 메트릭 (`WebSocketClientMetricsRecorder`, `MicrometerWebSocketClientMetricsRecorder`, `WebSocketClientMeters`, `WebSocketClientObservations` 등) | **없음** | 존재 | **1.3.7** |
| `HttpClientMetricsRecorder` 인터페이스 | — | **변화 없음** (메서드 diff 결과 동일) |
| `HttpClient` 새 deprecation | — | **없음** (`@Deprecated` 위치가 1.2와 동일한 5곳) |
| 번들 `reactorCoreVersion` | 3.7.19 | 3.8.7 | — |

`maxLifeTimeVariance(double)` 의 Javadoc이 쓸모를 명확히 설명한다 (`v1.3.7 ConnectionProvider.java:662-683`):

> Set a variance percentage for {@link #maxLifeTime(Duration)}, introducing per-connection jitter to prevent simultaneous expiry of connections created around the same time. The effective max lifetime for each connection will be in the range: `[maxLifeTime * (1 - variance/100), maxLifeTime]` … For example, with a {@code maxLifeTime} of 60 seconds and a variance of 2.5%, connections would expire between 58.5s and 60s. … Defaults to 0 (no variance — all connections expire at exactly {@code maxLifeTime}).

이것이 [`08-connection-reuse-pitfalls.md`](08-connection-reuse-pitfalls.md) 에서 다루는 "풀 전체가 동시에 만료돼 커넥션 폭풍이 나는" 문제의 1.3 전용 해법이다. 1.2에서는 `maxLifeTime` 을 서비스 인스턴스마다 다르게 주는 식으로 우회해야 했다.

`httpAuthentication(...)` 은 주의가 필요하다 — **reactor-netty 층에서 또 하나의 자동 재시도가 생긴다.** 401을 받으면 인증 헤더를 붙여 다시 보낸다(기본 `maxRetries = 1`, `v1.3.7 HttpClient.java:1789-1792`). [`09-resilience-integration.md`](09-resilience-integration.md) §2.3의 "시도 횟수가 곱으로 늘어난다" 문제가 **한 겹 더** 생긴다. WebClient에서는 같은 일을 `ExchangeFilterFunction` 으로 하는 편이 관측·테스트가 쉽다.

`recordPendingAcquireSuccess` / `recordPendingAcquireFailure` 는 커스텀 `MeterRegistrar` 가 **대기 지연(pending acquire latency)** 을 받을 수 있게 해 준다. 내부 브리지 클래스 `MeterRegistrarPoolMetricsRecorder` 의 Javadoc (`v1.3.7 MeterRegistrarPoolMetricsRecorder.java:24-29`):

> A {@link PoolMetricsRecorder} that bridges pending acquisition latencies to a user-provided {@link MeterRegistrar}. This allows custom registrars to receive the {@code pending.streams.time} data that is otherwise only available to the built-in Micrometer integration.

1.2에서는 커스텀 레지스트라가 이 데이터를 받을 수 없었다. default 메서드라서 기존 구현은 그대로 컴파일된다.

---

## 4. 실전 예제

### 4.1 제거된 API 치환

```java
// ─── exchange() ───────────────────────────────────────────────

// ❌ 7.0에서 컴파일 에러
Mono<Order> order = webClient.get().uri("/orders/{id}", id)
        .exchange()
        .flatMap(response -> response.bodyToMono(Order.class));
// 게다가 404/500에서 본문을 소비하지 않아 커넥션이 누수된다

// ✅ (a) 상태 분기가 필요 없으면 retrieve()
Mono<Order> order = webClient.get().uri("/orders/{id}", id)
        .retrieve()
        .bodyToMono(Order.class);

// ✅ (b) 응답 헤더/상태를 보고 분기해야 하면 exchangeToMono()
//     — 핸들러가 반환하는 Mono가 완료되면 Spring이 남은 본문을 자동 해제한다
Mono<Order> order = webClient.get().uri("/orders/{id}", id)
        .exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(Order.class);
            }
            if (response.statusCode().value() == 404) {
                return response.releaseBody().then(Mono.empty());
            }
            return response.createError();   // WebClientResponseException으로 변환
        });

// ─── syncBody() ───────────────────────────────────────────────
// ❌ webClient.post().uri("/orders").syncBody(command)
// ✅
webClient.post().uri("/orders").bodyValue(command);

// ─── context() ────────────────────────────────────────────────
// ❌ webClient.get().uri("/orders").context(ctx -> ctx.put("tenant", tenantId))
// ✅ 호출 결과에 붙인다 (하위 호출에도 전파된다)
webClient.get().uri("/orders")
        .retrieve().bodyToMono(Order.class)
        .contextWrite(ctx -> ctx.put("tenant", tenantId));

// ─── basicAuthentication() 무인자 + Credentials ───────────────
// ❌ 7.0에서 전부 제거
// .filter(ExchangeFilterFunctions.basicAuthentication())
// .attributes(ExchangeFilterFunctions.Credentials
//         .basicAuthenticationCredentials(user, password))

// ✅ 요청별 자격증명이 필요하면 필터를 직접 쓴다
static final String CREDENTIALS_ATTRIBUTE = "app.client.credentials";

ExchangeFilterFunction perRequestBasicAuth = (request, next) ->
        request.attribute(CREDENTIALS_ATTRIBUTE)
                .map(BasicCredentials.class::cast)
                .map(cred -> next.exchange(ClientRequest.from(request)
                        .headers(headers -> headers.setBasicAuth(
                                HttpHeaders.encodeBasicAuth(
                                        cred.username(), cred.password(), StandardCharsets.UTF_8)))
                        .build()))
                .orElseGet(() -> next.exchange(request));

// ✅ 고정 자격증명이면 7.0.8+ 오버로드 (UTF-8 지원)
WebClient client = builder
        .filter(ExchangeFilterFunctions.basicAuthentication(
                props.getUsername(), props.getPassword(), StandardCharsets.UTF_8))
        .build();

// ─── ReactorResourceFactory ───────────────────────────────────
// ❌ import org.springframework.http.client.reactive.ReactorResourceFactory;
// ✅ import org.springframework.http.client.ReactorResourceFactory;
//    (6.2에서 미리 바꿔 두면 7.0 업그레이드가 무사통과한다)
```

### 4.2 `@ImportHttpServices` 로 WebClient 그룹 등록 (7.0 전용)

```java
package com.example.client;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * @HttpExchange 인터페이스를 그룹 단위로 등록한다. (Spring Framework 7.0+)
 * clientType을 명시하지 않으면 RestClient(블로킹)가 쓰인다 — WebFlux에서는 치명적이다.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(
        group = "order",
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT,
        types = {OrderApi.class, OrderItemApi.class})
@ImportHttpServices(
        group = "payment",
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT,
        basePackageClasses = PaymentApi.class)
public class HttpServicesConfig {

    /**
     * 그룹별 WebClient.Builder 설정.
     * WebClientHttpServiceGroupAdapter는 WebClient.builder()를 맨 처음부터 만들므로
     * (createClientBuilder()), 애플리케이션의 커스터마이즈는 여기서 넣어야 한다.
     */
    @Bean
    WebClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(
            ConnectionProvider connectionProvider, ClientProperties props) {

        return groups -> {
            groups.filterByName("order").forEachClient((group, builder) ->
                    builder.baseUrl(props.getOrderBaseUrl())
                            .clientConnector(connector(connectionProvider, props.getOrderResponseTimeout())));

            groups.filterByName("payment").forEachClient((group, builder) ->
                    builder.baseUrl(props.getPaymentBaseUrl())
                            .clientConnector(connector(connectionProvider, props.getPaymentResponseTimeout())));
        };
    }

    private ReactorClientHttpConnector connector(ConnectionProvider provider, Duration responseTimeout) {
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
                .responseTimeout(responseTimeout)
                .metrics(true, uri -> uri);
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    ConnectionProvider sharedConnectionProvider(ClientProperties props) {
        return ConnectionProvider.builder("http-services")
                .maxConnections(props.getMaxConnections())
                .maxIdleTime(props.getMaxIdleTime())
                .maxLifeTime(props.getMaxLifeTime())
                // reactor-netty 1.3.4+ — 동시 만료를 분산시킨다
                .maxLifeTimeVariance(props.getMaxLifeTimeVariancePercent())
                .evictInBackground(Duration.ofSeconds(30))
                .metrics(true)
                .build();
    }
}
```

인터페이스와 사용:

```java
package com.example.client;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface OrderApi {

    @GetExchange("/orders/{id}")
    Mono<Order> findById(@PathVariable String id);
}
```

```java
package com.example.order;

import reactor.core.publisher.Mono;

import com.example.client.OrderApi;
import org.springframework.stereotype.Service;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

@Service
public class OrderService {

    private final OrderApi orderApi;

    // (a) 프록시 빈을 직접 주입 — 가장 흔한 방식
    public OrderService(OrderApi orderApi) {
        this.orderApi = orderApi;
    }

    // (b) 또는 레지스트리에서 그룹명으로 꺼낸다
    //     같은 타입이 여러 그룹에 있을 때는 그룹명이 필수다
    public OrderService(HttpServiceProxyRegistry registry) {
        this.orderApi = registry.getClient("order", OrderApi.class);
    }

    public Mono<Order> find(String id) {
        return this.orderApi.findById(id);
    }
}
```

404를 빈 `Mono` 로 받고 싶으면 어댑터를 감싼다:

```java
@Bean
WebClientHttpServiceGroupConfigurer notFoundTolerantGroups() {
    return groups -> groups.filterByName("order")
            .forEachProxyFactory((group, factoryBuilder) ->
                    factoryBuilder.exchangeAdapterDecorator(NotFoundWebClientAdapterDecorator::new));
}
```

### 4.3 API 버저닝 (7.0 전용)

```java
package com.example.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class VersionedClientConfig {

    @Bean
    WebClient catalogWebClient(WebClient.Builder builder, ClientProperties props) {
        return builder
                .baseUrl(props.getCatalogBaseUrl())
                // 버전을 어디에 넣을지 — 헤더/쿼리/경로 중 선택
                .apiVersionInserter(ApiVersionInserter.useHeader(props.getVersionHeaderName()))
                // 요청별로 지정하지 않으면 이 값이 쓰인다
                .defaultApiVersion(props.getDefaultApiVersion())
                .build();
    }
}
```

```java
// 요청별 오버라이드
catalogWebClient.get().uri("/products/{id}", id)
        .apiVersion("2024-11-01")
        .retrieve().bodyToMono(Product.class);

// 버전 삽입을 명시적으로 거부 (defaultApiVersion도 적용되지 않는다)
catalogWebClient.get().uri("/health")
        .apiVersion(null)
        .retrieve().toBodilessEntity();
```

```yaml
app:
  client:
    catalog:
      base-url: ${CATALOG_API_BASE_URL}
      version-header-name: X-API-Version
      default-api-version: "2024-11-01"
```

6.2에서 같은 일을 하려면 `defaultHeader(...)` + `ExchangeFilterFunction` 조합이었다. 7.0의 `ApiVersionInserter` 는 헤더/쿼리/경로 전략을 추상화하고 `RestClient` 와 공유한다.

### 4.4 Jackson 2 → 3 전환

```groovy
// build.gradle — Jackson 3로 이동
dependencies {
    // 제거: implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("tools.jackson.core:jackson-databind")
}
```

Jackson 2를 당장 유지해야 하면 **아무 것도 하지 않아도 7.0.9가 폴백해 준다**(§3.2). 단 코덱 접근자는 새 이름을 쓴다.

```java
package com.example.client;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class CodecConfig {

    @Bean
    WebClient.Builder jacksonConfiguredBuilder(JsonMapper jsonMapper, ClientProperties props) {
        DataSize maxInMemory = props.getMaxInMemorySize();

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize((int) maxInMemory.toBytes());
                    // 7.0: jackson2JsonDecoder(...) 는 deprecated — 새 이름을 쓴다
                    configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
                    configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
                })
                .build();

        return WebClient.builder().exchangeStrategies(strategies);
    }
}
```

전환 체크리스트:

1. `com.fasterxml.jackson.*` import를 `tools.jackson.*` 로 — Jackson 3는 패키지가 완전히 다르다
2. `ObjectMapper` → `JsonMapper` (Jackson 3의 기본 진입점)
3. `Jackson2JsonDecoder`/`Encoder` 직접 생성 코드를 `JacksonJsonDecoder`/`Encoder` 로
4. `CodecConfigurer` 호출을 `jackson2*` → `jacksonJson*` 로
5. **두 Jackson이 공존하면 `JACKSON_PRESENT` 가 우선**이므로, 의존성을 추가하는 순간 기본 코덱이 바뀐다. 커스텀 매퍼 설정이 새 코덱에도 적용되는지 반드시 확인
6. 직렬화 결과 스냅샷 테스트가 있으면 전부 돌려라 — Jackson 3는 일부 기본값이 다르다

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
10. Jackson 3 전환 (별도 작업으로 분리 — 직렬화 회귀 위험)
11. (선택) @ImportHttpServices 도입
12. (선택) apiVersionInserter 도입
13. (선택) maxLifeTimeVariance 설정
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
| `Http2AllocationStrategy.strictConnectionReuse` | 없음 | 있음 | 1.3.3 |
| `Http2AllocationStrategy.streamBatchSize` | 없음 | 있음 | 1.3.4 |
| WebSocket 클라이언트 메트릭 | 없음 | 있음 | 1.3.7 |
| `HttpClientMetricsRecorder` / `HttpClient` deprecation | — | **변화 없음** | — |

### 5.2 소스에서 확인되지 않아 문서에 넣지 않은 항목

| 항목 | 이유 |
|---|---|
| Spring Boot 4의 자동 설정·프로퍼티 변경 | Boot 소스 워크트리가 없어 확인 불가. 이 문서는 Framework 레벨만 다룬다 |
| `boundedElastic` 가상 스레드 옵션 | **델타가 아니다.** `Schedulers` Javadoc에 "Since 3.6.0"으로 적혀 있고 3.7.19에도 `reactor.schedulers.defaultBoundedElasticOnVirtualThreads` 시스템 프로퍼티가 이미 있다 |
| Java 베이스라인 상향 | 7.0.9 빌드의 `DEFAULT_RELEASE_VERSION` 이 17로 남아 있어(툴체인은 25) 바이트코드 타깃 변경을 단정할 수 없다 |
| `CodecConfigurer` 의 `decoder`/`encoder`/`reader`/`writer`/`withDefaultCodecConfig`/`isEnableLoggingRequestDetails` 제거 여부 | 시그니처 grep 결과로만 차이가 보여 Javadoc 확인이 끝나지 않았다. Jackson 관련 변경만 기재 |
| reactor-netty의 Netty 버전 상향 | `nettyDefaultVersion` 이 `gradle.properties` 에 없어 확인 불가 |
| Spring Boot 4가 `@ImportHttpServices` 그룹에 자동으로 꽂아 주는 설정(관측·메트릭·로드밸런서 등) | Boot 소스가 없어 확인 불가. §4.2는 Framework만으로 설정하는 형태로 썼다 |

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
