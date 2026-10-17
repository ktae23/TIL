# 관측성 — WebClient 호출을 운영에서 들여다보는 법

WebClient 호출 하나는 Spring의 Micrometer Observation과 reactor-netty의 채널 메트릭이라는 **서로 다른 두 계측 체계**를 통과한다. 각 체계가 무엇을 기록하고, 무엇을 켜야 하며, 무엇을 프로덕션에 켜면 안 되는지를 소스 기준으로 정리한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

```
      애플리케이션
          │  ┌──────────────────────────────────────────────────────┐
          ├──┤ ① Spring Observation — http.client.requests          │
          │  │    DefaultWebClient.exchange() 가 start/stop         │
          │  │    태그: method, uri, status, client.name,            │
          │  │          exception, outcome (+ high: http.url)       │
          │  └──────────────────────────────────────────────────────┘
      WebClient
          │  ┌──────────────────────────────────────────────────────┐
          ├──┤ ② ExchangeFunctions 로거 (DEBUG/TRACE)               │
          │  │   org.springframework.web.reactive.function          │
          │  │       .client.ExchangeFunctions                      │
          │  └──────────────────────────────────────────────────────┘
    reactor-netty
          │  ┌──────────────────────────────────────────────────────┐
          ├──┤ ③ HttpClient 메트릭 — reactor.netty.http.client.*     │
          ├──┤ ④ 커넥션 풀 메트릭 — reactor.netty.connection.provider.*│
          └──┤ ⑤ wiretap — 기본 카테고리                             │
             │    reactor.netty.http.client.HttpClient               │
             └──────────────────────────────────────────────────────┘
                              ③④⑤ 모두 기본 비활성
```

`DefaultClientRequestObservationConvention` 의 기본 관측 이름은 `"http.client.requests"` (`DefaultClientRequestObservationConvention.java:42`). `ClientHttpObservationDocumentation.HTTP_REACTIVE_CLIENT_EXCHANGES` 가 선언하는 태그 전체:

| 태그 | 카디널리티 | 값 | 소스 |
|---|---|---|---|
| `method` | low | `GET`, `POST`, … / 요청 생성 실패 시 `none` | `method()` `:121` |
| `uri` | low | **URI 템플릿의 path 부분** / 없으면 `none`, 루트면 `/` | `uri()` `:105` |
| `status` | low | 응답 상태코드 문자열 / `IO_ERROR` / `CLIENT_ERROR` | `status()` `:130` |
| `client.name` | low | 요청 URI의 host (`@since 6.0.5`) / 없으면 `none` | `clientName()` `:144` |
| `exception` | low | 예외 `getSimpleName()` / 없으면 `none` | `exception()` `:151` |
| `outcome` | low | `SUCCESS` / `HttpStatus.Series` 이름 / `UNKNOWN` | `outcome()` `:161` |
| `http.url` | **high** | 전체 URI(`toASCIIString()`) | `httpUrl()` `:177` |

`getContextualName()` 은 `"http " + 메서드 소문자` 를 돌려준다(`:94-97`) — 트레이싱 span 이름이 된다.

---

## 2. 왜 알아야 하는가 (Why)

### 2.1 `uri` 태그가 템플릿이 아니면 정보를 잃는다

```java
// DefaultClientRequestObservationConvention.uri() — :105-114
if (context.getUriTemplate() != null) {
    return KeyValue.of(LowCardinalityKeyNames.URI, extractPath(context.getUriTemplate()));
}
ClientRequest request = context.getRequest();
if (request != null && ROOT_PATH.equals(request.url().getPath())) {
    return URI_ROOT;
}
return URI_NONE;
```

템플릿이 없으면 실제 URI를 쓰지 않고 `none` 을 쓴다 — Spring 쪽 `uri` 태그는 카디널리티 폭발로부터 **안전하지만 정보를 잃는다**.

```java
// ✅ uri = "/orders/{id}"          ❌ uri = "none"
webClient.get().uri("/orders/{id}", id)   |   webClient.get().uri("/orders/" + id)
```

템플릿은 `uri(String, Object...)` 호출 시 요청 속성 `WebClient.class.getName() + ".uriTemplate"` 에 저장되고, `exchange()` 가 컨텍스트로 옮긴다(§3.1).

**반면 reactor-netty의 `uri` 태그는 안전하지 않다.** `metrics(true, uriTagValue)` 의 `uriTagValue` 가 `Function.identity()` 면 실제 URI가 그대로 태그가 되고, DEBUG 로그로 경고만 한다(`HttpClient.java:1252-1256`):

```java
if (uriTagValue == Function.<String>identity()) {
    log.debug("Metrics are enabled with [uriTagValue=Function#identity]. " +
            "It is strongly recommended to provide template-like form for the URIs. ...");
}
```

막지는 않는다. 그래서 reactor-netty 메트릭을 켤 때는 반드시 **(a) 템플릿 변환 함수를 넘기고 (b) `MeterFilter.maximumAllowableTags` 로 상한을 걸어야** 한다. Javadoc이 직접 그 예시를 권한다(`HttpClient.java:1235`).

### 2.2 `status` / `outcome` 이 IO 에러·취소를 표현하는 방식

```java
// status() — DefaultClientRequestObservationConvention.java:130-142
if (context.isAborted())                                    return STATUS_CLIENT_ERROR;  // "CLIENT_ERROR"
ClientResponse response = context.getResponse();
if (response != null)                                       return String.valueOf(response.statusCode().value());
if (context.getError() != null && context.getError() instanceof IOException)
                                                            return STATUS_IO_ERROR;      // "IO_ERROR"
return STATUS_CLIENT_ERROR;
```

| 상황 | `status` | `outcome` |
|---|---|---|
| 200 응답 | `200` | `SUCCESS` |
| 404 응답 | `404` | `CLIENT_ERROR` (= `HttpStatus.Series.CLIENT_ERROR`) |
| 503 응답 | `503` | `SERVER_ERROR` |
| `IOException`(연결 실패, read timeout) | `IO_ERROR` | `UNKNOWN` |
| 응답 전 취소 | `CLIENT_ERROR` | `UNKNOWN` |
| 비 IO 예외(디코딩 실패 등) | `CLIENT_ERROR` | `UNKNOWN` |

함정: **`status=CLIENT_ERROR` 는 4xx가 아니다.** 4xx는 `status=404` 처럼 숫자로 나오고 `outcome=CLIENT_ERROR` 가 된다. `status` 의 문자열 `CLIENT_ERROR` 는 "클라이언트 측에서 교환이 끝나버렸다"는 뜻이다. 대시보드를 `status=~"4.."` 로 짜면 취소·예외가 빠지고, `outcome="CLIENT_ERROR"` 로 짜면 4xx 응답과 섞인다(§4.4에 분리 쿼리).

### 2.3 wiretap을 프로덕션에 켜면 안 되는 이유

`wiretap()` 은 netty 파이프라인에 `LoggingHandler` 를 꽂아 바이트 단위로 모든 프레임을 로그에 쓴다.

1. **민감정보 전량 유출.** `Authorization` 헤더, 쿠키, 요청·응답 본문이 전부 남는다. 마스킹 기능이 없다. Spring 쪽 `ExchangeFunctions` 로거는 헤더를 `{masked}` 로 가리지만(`ExchangeFunctions.java:139`) wiretap에는 그런 보호가 없다.
2. **성능.** 기본 포맷이 `HEX_DUMP` 라 1KB 응답이 수십 줄 hex dump가 된다. 로그 I/O와 문자열 생성이 이벤트 루프 스레드에서 일어난다.
3. **디스크.** 초당 수백 요청이면 분 단위로 GB가 쌓인다.

운영에서 필요한 것은 wiretap이 아니라 **①의 메트릭 + ②의 DEBUG 로거**다. wiretap은 재현 가능한 이슈를 로컬/스테이징에서 파고들 때만 켠다.

---

## 3. 내부 구현 분석 (How)

### 3.1 Observation의 생애 — carrier가 빌더인 이유

`DefaultWebClient.DefaultRequestBodyUriSpec.exchange()` 가 관측의 전체 생애를 관리한다 (`DefaultWebClient.java:450-490`).

```mermaid
sequenceDiagram
    participant S as exchange()
    participant O as Observation
    participant C as ClientRequestObservationContext
    participant B as ClientRequest.Builder
    participant F as ObservationFilterFunction
    participant E as ExchangeFunction

    S->>B: initRequestBuilder()
    S->>S: Mono.deferContextual(...)  ← 구독마다 실행
    S->>C: new ClientRequestObservationContext(requestBuilder)
    C->>B: setCarrier(builder) / setRequest(builder.build())
    S->>O: observation(...).parentObservation(ctx[OTLA.KEY]).start()
    Note over O,B: ObservationHandler가 carrier(=빌더)에<br/>트레이싱 헤더를 주입
    S->>B: request = requestBuilder.build()  ← 주입된 헤더 포함
    S->>C: setUriTemplate(attr), setRequest(request)
    S->>F: filterFunctions.andThen(ObservationFilterFunction)
    F->>E: exchange(request)
    E-->>F: ClientResponse
    F->>C: setResponse(response)
    S->>O: doOnError → error(t)
    S->>O: doFinally → (CANCEL && !received) ? setAborted(true) : — ; stop()
    Note over S: contextWrite(OTLA.KEY → observation)
```

**설계 포인트 — carrier가 불변 `ClientRequest` 가 아니라 가변 빌더다.**

```java
public class ClientRequestObservationContext
        extends RequestReplySenderContext<ClientRequest.Builder, ClientResponse> {
```

Javadoc이 이유를 명시한다(`ClientRequestObservationContext.java:27-30`):

> The {@link #getCarrier() tracing context carrier} is a {@link ClientRequest.Builder request builder}, since the actual request is immutable. For {@code KeyValue} extraction, the {@link #getRequest() actual request} should be used instead.

트레이싱 라이브러리는 `observation.start()` 시점에 carrier에 `traceparent` 같은 헤더를 **써야** 한다. `ClientRequest` 는 불변이라 쓸 수 없다. 그래서 carrier는 빌더이고 setter는 이렇게 생겼다(`:63-67`):

```java
private static void setRequestHeader(@Nullable ClientRequest.Builder request, String name, String value) {
    if (request != null) {
        request.headers(headers -> headers.set(name, value));
    }
}
```

그리고 **`start()` 호출 뒤에 `requestBuilder.build()`** 가 실행되므로(`DefaultWebClient.java:457` → `:462`) 주입된 헤더가 실제 요청에 들어간다. 순서가 뒤바뀌면 트레이싱 헤더가 사라진다. 태그 추출은 불변 `getRequest()` 를 쓴다 — carrier는 쓰기용, request는 읽기용으로 역할이 분리돼 있다.

Spring 자신의 테스트가 이 주입 지점을 검증한다 (`WebClientObservationTests.HeaderInjectingHandler`, `:205-216`):

```java
@Override
public void onStart(ClientRequestObservationContext context) {
    context.getSetter().set(context.getCarrier(), "foo", "bar");
}
```

### 3.2 응답과 취소의 기록 지점

**응답**은 필터 체인 가장 안쪽에 삽입되는 `ObservationFilterFunction` 이 심는다 (`DefaultWebClient.java:737-754`):

```java
Mono<ClientResponse> exchange = next.exchange(request);
if (this.observationContext instanceof ClientRequestObservationContext clientContext) {
    exchange = exchange.doOnNext(clientContext::setResponse);
}
```

`filterFunctions.andThen(filterFunction)` 으로 **사용자 필터 뒤에** 붙으므로(`:458-461`), 사용자 필터가 응답을 바꿔치기해도 관측은 원본을 본다.

**취소**는 `doFinally` 에서 판정한다 (`:480-487`):

```java
final AtomicBoolean responseReceived = new AtomicBoolean();
return responseMono
        .doOnNext(response -> responseReceived.set(true))
        .doOnError(observation::error)
        .doFinally(signalType -> {
            if (signalType == SignalType.CANCEL && !responseReceived.get() &&
                    observation.getContext() instanceof ClientRequestObservationContext observationContext) {
                observationContext.setAborted(true);
            }
            observation.stop();
        })
        .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
```

`setAborted(true)` 는 **응답을 받기 전에 취소된 경우만**이다. 응답 수신 후 본문을 일부만 읽고 취소하면(`bodyToFlux(...).take(1)`) `aborted` 가 false로 남아 `status=200`, `outcome=SUCCESS` 가 된다 — Spring의 `recordsObservationForCancelledExchangeDuringResponse` 가 이 동작을 고정한다(`WebClientObservationTests.java:136-143`).

### 3.3 Observation은 Reactor Context를 타고 전파된다

```java
// 들어오는 방향 — DefaultWebClient.java:452-457
return Mono.deferContextual(contextView -> {
    Observation observation = ...;
    observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

// 나가는 방향 — :488
.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
```

키는 `io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor.KEY` 다. 상위 호출자가 같은 키로 부모 관측을 넣어 두면 자동으로 부모-자식 관계가 생기고, `Hooks.enableAutomaticContextPropagation()` 을 켜면 ThreadLocal ↔ Reactor Context 자동 변환이 붙어 MDC·`@Observed` 와도 이어진다.

중요: **관측 생성이 `Mono.deferContextual` 안에 있다** → 조립 시점이 아니라 **구독 시점**에 만들어진다. [`09-resilience-integration.md`](09-resilience-integration.md) 의 애플리케이션 재시도는 재구독이므로 시도마다 새 관측이 시작된다 — 메트릭의 호출 수가 "논리적 요청 수"가 아니라 "실제 HTTP 시도 수"가 되는 이유다. Reactor Context 전파의 일반 원리는 [`../main/09-context-propagation.md`](../main/09-context-propagation.md).

### 3.4 reactor-netty 메트릭

`HttpClient.metrics(boolean, Function<String,String>)` (`HttpClient.java:1245`):

```java
if (enable) {
    if (!Metrics.isMicrometerAvailable() && !Metrics.isTracingAvailable()) {
        throw new UnsupportedOperationException(
                "To enable metrics, you must add the dependencies to `io.micrometer:micrometer-core`" +
                        " and `io.micrometer:micrometer-tracing` to the class path first");
    }
    ...
    dup.configuration().metricsRecorder(() -> configuration().defaultMetricsRecorder());
```

기본 레코더는 `HttpClientConfig.defaultMetricsRecorder()` 가 제공한다 (`HttpClientConfig.java:487-490`): `return MicrometerHttpClientMetricsRecorder.INSTANCE;`

주의: `MicrometerHttpClientMetricsRecorder` 는 **package-private** 이다 (`final class … implements HttpClientMetricsRecorder`, `MicrometerHttpClientMetricsRecorder.java:55`). 애플리케이션 코드에서 직접 참조할 수 없다. 커스터마이징하려면 공개 인터페이스 **`HttpClientMetricsRecorder`** 를 구현해 `metrics(true, Supplier<? extends ChannelMetricsRecorder>, Function)` (`HttpClient.java:1295`) 로 넘긴다. (`HttpClientMetrics` 라는 타입은 없다.)

`HttpClientMetricsRecorder` 의 기록 지점: `recordDataReceivedTime` / `recordDataSentTime` / `recordResponseTime`, 상위 `ChannelMetricsRecorder` 의 `recordDataReceived`·`recordDataSent`·`recordConnectTime`·`recordTlsHandshakeTime`·`recordResolveAddressTime`. `@since 1.1.17` 로 `proxyAddress` 를 받는 오버로드가 default 메서드로 추가되어 기존 구현은 그대로 동작한다.

미터 이름은 `reactor.netty.Metrics` 상수 조합이다 — 접두는 `reactor.netty.http.client` (`HTTP_CLIENT_PREFIX`):

| 미터 | 전체 이름 |
|---|---|
| 전체 응답 시간 | `reactor.netty.http.client.response.time` |
| 본문 수신/전송 시간 | `….data.received.time` / `….data.sent.time` |
| 수신/전송 바이트 | `….data.received` / `….data.sent` |
| 에러 수 | `….errors` |
| 연결 / TLS / DNS | `….connect.time` / `….tls.handshake.time` / `….address.resolver` |

태그는 `remote.address`, `proxy.address`, `uri`, `method`, `status` (프록시 없으면 `proxy.address=na`).

### 3.5 커넥션 풀 메트릭

별개 스위치다. `ConnectionProvider.Builder.metrics(boolean)` (`ConnectionProvider.java:694`) — Javadoc에 `By default this is not enabled` 명시. `MicrometerPooledConnectionProviderMeterRegistrar` 가 등록하는 게이지 (`:54-74`), 접두는 `reactor.netty.connection.provider`:

| 미터 | `PoolMetrics` 소스 | 의미 |
|---|---|---|
| `.total.connections` | `allocatedSize()` | 풀이 보유한 커넥션 수 |
| `.active.connections` | `acquiredSize()` | 현재 사용 중 |
| `.idle.connections` | `idleSize()` | 유휴 |
| `.pending.connections` | `pendingAcquireSize()` | **acquire 대기 중인 요청 수** |
| `.max.connections` | `getMaxAllocatedSize()` | 설정된 상한 |
| `.max.pending.connections` | `getMaxPendingAcquireSize()` | 대기 큐 상한 |

태그는 `id`, `name`, `remote.address` (`ConnectionProviderMeters.ConnectionProviderMetersTags`).

**운영에서 가장 먼저 봐야 하는 것은 `pending.connections` 다.** 0을 벗어나기 시작하면 풀이 포화된 것이고 곧 `pendingAcquireTimeout` 초과가 터진다 — `active / max` 비율보다 선행 지표다. 풀 동작 자체는 [`06-connection-pool.md`](06-connection-pool.md).

### 3.6 wiretap의 오버로드와 기본 카테고리

`Transport.wiretap(...)` (`Transport.java`):

| 시그니처 | 줄 | 카테고리 | 레벨 | 포맷 |
|---|---|---|---|---|
| `wiretap(boolean)` | `:275` | **기본 핸들러** | `DEBUG` | `HEX_DUMP` |
| `wiretap(String)` | `:301` | 지정 | `DEBUG` | `HEX_DUMP` |
| `wiretap(String, LogLevel)` | `:315` | 지정 | 지정 | `HEX_DUMP` |
| `wiretap(String, LogLevel, AdvancedByteBufFormat)` | `:339` | 지정 | 지정 | 지정 (charset은 `Charset.defaultCharset()`) |
| `wiretap(String, LogLevel, AdvancedByteBufFormat, Charset)` | `:366` | 지정 | 지정 | 지정 (charset도 지정) |

`wiretap(true)` 는 `configuration().defaultLoggingHandler()` 를 쓴다(`:278`). HTTP 클라이언트의 그것은 (`HttpClientConfig.java:876-878`):

```java
static final LoggingHandler LOGGING_HANDLER =
        AdvancedByteBufFormat.HEX_DUMP
                .toLoggingHandler(HttpClient.class.getName(), LogLevel.DEBUG, Charset.defaultCharset());
```

따라서 **기본 wiretap 로거 카테고리는 `reactor.netty.http.client.HttpClient`, 레벨 DEBUG, 포맷 HEX_DUMP** 다. (TCP 전송 계층은 `reactor.netty.tcp.TcpClient` — `TcpClientConfig.java:137-139`.)

`AdvancedByteBufFormat` 세 값:

| 값 | 기록 내용 | 쓸 곳 |
|---|---|---|
| `SIMPLE` | 이벤트만 (`REGISTERED`, `ACTIVE`, `READ: 145B`, `WRITE: 38B`) | 커넥션 재사용·종료 추적. 본문이 안 남아 비교적 안전 |
| `HEX_DUMP` | 이벤트 + 본문 hex dump (기본값) | 바이너리 프로토콜 디버깅 |
| `TEXTUAL` | 이벤트 + 본문 평문 | JSON API 디버깅. **민감정보 평문 노출** |

`toLoggingHandler()` 는 netty 기본 핸들러가 아니라 reactor-netty의 `ReactorNettyLoggingHandler` 를 만든다(`AdvancedByteBufFormat.java:105-114`) — 채널 ID와 로컬/리모트 주소를 접두로 붙여 멀티플렉싱된 로그를 구분하게 한다.

**클라이언트 측 `AccessLog` 는 없다.** reactor-netty의 access log는 서버 전용이다. 클라이언트에서 "요청 한 줄 로그"가 필요하면 ①의 Observation, ②의 `ExchangeFunctions` DEBUG 로거, 또는 `ExchangeFilterFunction` 을 쓴다([`04-exchange-filter-function.md`](04-exchange-filter-function.md)).

---

## 4. 실전 예제

### 4.1 운영에서 켜는 설정

```java
package com.example.client.observability;

import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.ObservationRegistry;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static reactor.netty.Metrics.CONNECTION_PROVIDER_PREFIX;
import static reactor.netty.Metrics.HTTP_CLIENT_PREFIX;
import static reactor.netty.Metrics.REMOTE_ADDRESS;
import static reactor.netty.Metrics.URI;

@Configuration(proxyBeanMethods = false)
public class ObservabilityConfig {

    /** /orders/42 → /orders/{id} 처럼 실제 URI를 템플릿으로 되돌린다. */
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+(?=/|$)");
    private static final Pattern UUID_SEGMENT = Pattern.compile(
            "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");

    /** 템플릿 변환이 놓친 패턴이 있어도 메트릭 카디널리티가 터지지 않게 상한을 건다. */
    @Bean
    MeterFilter reactorNettyUriTagLimit() {
        return MeterFilter.maximumAllowableTags(HTTP_CLIENT_PREFIX, URI, 100, MeterFilter.deny());
    }

    @Bean
    MeterFilter reactorNettyPoolTagLimit() {
        // 풀 메트릭의 remote.address 태그는 DNS 라운드로빈이면 늘어날 수 있다
        return MeterFilter.maximumAllowableTags(
                CONNECTION_PROVIDER_PREFIX, REMOTE_ADDRESS, 50, MeterFilter.deny());
    }

    @Bean
    ConnectionProvider orderConnectionProvider(OrderClientProperties props) {
        return ConnectionProvider.builder("order-api")
                .maxConnections(props.getMaxConnections())
                .pendingAcquireMaxCount(props.getPendingAcquireMaxCount())
                .pendingAcquireTimeout(props.getPendingAcquireTimeout())
                .maxIdleTime(props.getMaxIdleTime())
                .maxLifeTime(props.getMaxLifeTime())
                .evictInBackground(Duration.ofSeconds(30))
                .metrics(true)                      // ④ 풀 메트릭 — 기본 비활성
                .build();
    }

    @Bean
    WebClient orderWebClient(WebClient.Builder builder, ConnectionProvider provider,
                             ObservationRegistry observationRegistry, OrderClientProperties props) {

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(props.getResponseTimeout())
                // ③ 기본 비활성. 반드시 템플릿 변환기를 넘긴다
                .metrics(true, templatize());
                // ⑤ wiretap은 켜지 않는다 (§2.3)

        return builder
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .observationRegistry(observationRegistry)   // ①
                .build();
    }

    private static Function<String, String> templatize() {
        return uri -> {
            String path = UUID_SEGMENT.matcher(uri).replaceAll("/{uuid}");
            return NUMERIC_SEGMENT.matcher(path).replaceAll("/{id}");
        };
    }
}
```

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.client.requests: true
      slo:
        http.client.requests: 50ms,100ms,200ms,500ms,1s,2s
  observations:
    key-values:
      application: ${spring.application.name}
```

### 4.2 커스텀 Observation convention

`client.name` 이 host라서 멀티테넌트에서 쓸모가 없거나, 비즈니스 태그를 붙여야 할 때.

```java
package com.example.client.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;

/** 논리적 서비스 이름 태그를 추가하고, 고카디널리티 http.url을 제거한다. */
public class ServiceNameObservationConvention extends DefaultClientRequestObservationConvention {

    public static final String SERVICE_ATTRIBUTE =
            ServiceNameObservationConvention.class.getName() + ".service";

    private static final KeyValue SERVICE_NONE = KeyValue.of("service", "none");

    public ServiceNameObservationConvention() {
        super("http.client.requests");   // 이름은 유지 — 대시보드 호환
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(service(context));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
        // 전체 URL은 쿼리에 토큰이 섞이기도 하므로 트레이싱에도 남기지 않는다
        return KeyValues.empty();
    }

    private KeyValue service(ClientRequestObservationContext context) {
        ClientRequest request = context.getRequest();
        if (request == null) {
            return SERVICE_NONE;
        }
        return request.attribute(SERVICE_ATTRIBUTE)
                .map(value -> KeyValue.of("service", String.valueOf(value)))
                .orElse(SERVICE_NONE);
    }
}
```

```java
WebClient client = builder.observationRegistry(registry)
        .observationConvention(new ServiceNameObservationConvention()).build();

client.get().uri("/orders/{id}", orderId)
        .attribute(ServiceNameObservationConvention.SERVICE_ATTRIBUTE, "order-api")
        .retrieve().bodyToMono(Order.class);
```

넘긴 convention이 우선하고, 없으면 `DEFAULT_OBSERVATION_CONVENTION` 이 쓰인다(`DefaultWebClient.java:80-81`, `:453-454`).

### 4.3 로거 설정 — wiretap은 전용 카테고리로 분리

```xml
<!-- logback-spring.xml -->
<configuration>
  <springProfile name="prod">
    <!-- 요청/응답 한 줄 요약. 헤더는 {masked}로 가려진다 -->
    <logger name="org.springframework.web.reactive.function.client.ExchangeFunctions" level="DEBUG"/>
    <!-- 커넥션 풀 경고(acquire timeout, eviction) -->
    <logger name="reactor.netty.resources" level="WARN"/>
    <!-- retry-once가 포기할 때의 WARN (HttpClientConnect.java:388) -->
    <logger name="reactor.netty.http.client" level="WARN"/>
  </springProfile>

  <springProfile name="local">
    <logger name="reactor.netty.resources.PooledConnectionProvider" level="DEBUG"/>
    <!-- wiretap을 전용 카테고리로 켠 경우에만 이 줄을 올린다 -->
    <logger name="com.example.client.wiretap" level="DEBUG"/>
  </springProfile>
</configuration>
```

```java
/** wiretap은 로컬/스테이징 전용. 전용 카테고리라 다른 netty 로그와 독립적으로 제어된다. */
@Configuration(proxyBeanMethods = false)
@Profile({"local", "staging"})
@ConditionalOnProperty("app.client.wiretap.enabled")
public class WiretapConfig {

    static final String WIRETAP_CATEGORY = "com.example.client.wiretap";

    static HttpClient apply(HttpClient httpClient) {
        return httpClient.wiretap(WIRETAP_CATEGORY, LogLevel.DEBUG,
                // 운영 데이터가 흐르는 환경에서는 SIMPLE을 쓸 것 — TEXTUAL은 본문 평문
                AdvancedByteBufFormat.TEXTUAL, StandardCharsets.UTF_8);
    }
}
```

함정: `wiretap(true)` 로 기본 카테고리를 쓰고 `reactor.netty.http.client` 를 DEBUG로 올리면 와이어 로그가 쏟아진다. 카테고리를 반드시 분리하라.

### 4.4 대시보드 쿼리

```promql
# 엔드포인트별 p99 (uri가 none이면 템플릿을 안 쓴 호출 — 먼저 고쳐라)
histogram_quantile(0.99,
  sum by (le, uri) (rate(http_client_requests_seconds_bucket{client_name="order-api"}[5m])))

# 실제 4xx 응답 / 응답을 못 받은 호출 / IO 실패 — §2.2의 세 가지를 분리한다
sum(rate(http_client_requests_seconds_count{status=~"4.."}[5m]))
sum(rate(http_client_requests_seconds_count{status="CLIENT_ERROR"}[5m]))
sum(rate(http_client_requests_seconds_count{status="IO_ERROR"}[5m]))

# 풀 포화 선행 지표 — 0을 벗어나면 경보
max by (name) (reactor_netty_connection_provider_pending_connections)

# Spring이 센 호출 수 vs reactor-netty가 센 응답 수.
# 차이는 커넥션 획득 실패, DNS 실패, netty 자동 재시도에서 나온다
sum(rate(http_client_requests_seconds_count[5m]))
- sum(rate(reactor_netty_http_client_response_time_seconds_count[5m]))
```

마지막 쿼리가 유용하다 — ①은 "WebClient 호출 시도", ③은 "실제로 응답을 받은 HTTP 교환"을 센다([`09-resilience-integration.md`](09-resilience-integration.md) §3.2).

---

## 5. 정리

| 항목 | 결론 |
|---|---|
| 관측 이름 | `http.client.requests` (`DefaultClientRequestObservationConvention.java:42`) |
| low cardinality 태그 | `method`, `uri`, `status`, `client.name`, `exception`, `outcome` |
| high cardinality 태그 | `http.url` — 트레이싱에만, 메트릭엔 넣지 말 것 |
| `uri` 태그 | 템플릿이 없으면 `none`. Spring 쪽은 안전하지만 정보를 잃는다 → 항상 `uri(template, args)` |
| `status=CLIENT_ERROR` | 4xx가 **아니다**. 응답 전 취소 또는 비 IO 예외 |
| `status=IO_ERROR` | `IOException` — 연결 실패, read timeout |
| carrier 설계 | 가변 `ClientRequest.Builder`. 불변 요청엔 트레이싱 헤더를 못 쓰기 때문 |
| 태그 추출 소스 | 불변 `getRequest()`. carrier는 쓰기용, request는 읽기용 |
| 관측 생성 시점 | `Mono.deferContextual` 안 → **구독마다**. 재시도 시 시도마다 관측이 생긴다 |
| Context 전파 키 | `ObservationThreadLocalAccessor.KEY` — 양방향 |
| reactor-netty 메트릭 | **기본 비활성**. `metrics(true, uriTagValue)` + `MeterFilter.maximumAllowableTags` 필수 |
| 레코더 타입 | 공개 인터페이스는 `HttpClientMetricsRecorder`. `MicrometerHttpClientMetricsRecorder` 는 package-private |
| 풀 메트릭 | 별개 스위치 `ConnectionProvider.Builder.metrics(true)`, 기본 비활성. `pending.connections` 가 선행 지표 |
| wiretap 기본 카테고리 | `reactor.netty.http.client.HttpClient`, DEBUG, `HEX_DUMP` (`HttpClientConfig.java:876-878`) |
| wiretap 포맷 | `SIMPLE`(이벤트만) / `HEX_DUMP`(기본) / `TEXTUAL`(평문 — 민감정보 노출) |
| 프로덕션 wiretap | **금지**. 민감정보 유출 + 이벤트 루프 부하 + 디스크. 전용 카테고리로 분리해 로컬에서만 |
| 클라이언트 AccessLog | **존재하지 않음**(서버 전용). 필터나 `ExchangeFunctions` DEBUG로 대체 |

---

## 관련 문서
- 선행: [재시도와 복원력](09-resilience-integration.md)
- 선행: [커넥션 풀](06-connection-pool.md)
- 선행: [ExchangeFilterFunction](04-exchange-filter-function.md)
- 후행: [WebClient 테스트](11-testing-webclient.md)
- 참조: [Reactor — Context 전파](../main/09-context-propagation.md)

---
*참고: Spring Framework 6.2.19 / reactor-netty 1.2.18 / Reactor Core 3.7.19 기준*
