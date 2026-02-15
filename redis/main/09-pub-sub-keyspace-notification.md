# Pub/Sub과 Keyspace Notification: 실시간 메시징의 내부 동작

Redis Pub/Sub은 발행-구독 패턴을 통해 클라이언트 간 실시간 메시지를 전달하며, Keyspace Notification은 키의 상태 변화를 이벤트로 감지할 수 있게 해준다. 이 문서에서는 Pub/Sub의 내부 구현 구조, 메시지 전달 보장 수준, Keyspace Notification 설정과 활용, 그리고 Stream과의 비교를 분석한다.

## 목차

1. [핵심 개념 (What)](#1-핵심-개념-what)
2. [왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
3. [내부 구현 분석 (How)](#3-내부-구현-분석-how)
4. [실전 예제](#4-실전-예제)
5. [정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Pub/Sub이란?

Pub/Sub(Publish/Subscribe)은 메시지 발행자(Publisher)와 구독자(Subscriber)가 직접 연결되지 않고, **채널(Channel)**을 매개로 메시지를 주고받는 비동기 메시징 패턴이다. Redis는 이 패턴을 서버 레벨에서 네이티브로 지원한다.

### 핵심 구성요소

| 구성요소 | 역할 |
|---------|------|
| `SUBSCRIBE` | 특정 채널을 구독하여 메시지를 수신 대기 |
| `PUBLISH` | 특정 채널에 메시지를 발행 |
| `PSUBSCRIBE` | glob 패턴으로 여러 채널을 동시에 구독 |
| `UNSUBSCRIBE` / `PUNSUBSCRIBE` | 채널 또는 패턴 구독 해제 |
| `pubsub_channels` | 채널명을 키, 구독 클라이언트 리스트를 값으로 갖는 dict |
| `pubsub_patterns` | 패턴-클라이언트 매핑을 저장하는 linked list |
| Keyspace Notification | 키에 대한 명령 실행 시 `__keyevent__@<db>__` 채널로 이벤트 발행 |

### 메시지 전달 보장 수준

| 보장 수준 | 설명 |
|----------|------|
| **At-most-once** | 메시지는 최대 한 번 전달되며, 유실 가능 |
| 구독자 부재 시 | 해당 메시지는 **영구적으로 유실**됨 |
| 재연결 시 | 연결 끊김 동안의 메시지를 **복구할 수 없음** |
| 버퍼 초과 시 | 느린 구독자의 output buffer가 초과하면 연결이 강제 종료됨 |

## 2. 왜 알아야 하는가 (Why)

### 실무에서 만나는 상황

1. **실시간 알림 시스템**: 채팅, 실시간 대시보드, 알림 푸시 등에서 Pub/Sub을 활용하면 별도의 메시지 브로커 없이 빠른 실시간 통신이 가능하다.

2. **캐시 무효화 전파**: 분산 환경에서 하나의 노드가 캐시를 갱신하면 다른 노드에게 즉시 알려 로컬 캐시를 무효화해야 한다. Pub/Sub은 이 용도에 적합하다.

3. **캐시 만료 감지**: TTL이 설정된 키가 만료되었을 때 후속 처리(DB 동기화, 로그 기록 등)를 해야 하는 경우 Keyspace Notification이 필수적이다.

4. **Stream과의 선택**: 메시지 유실이 허용되는 Fire-and-forget 시나리오와 메시지 보존이 필요한 시나리오를 구분하여 Pub/Sub과 Stream 중 올바른 선택을 해야 한다.

## 3. 내부 구현 분석 (How)

### 3.1 전체 아키텍처 다이어그램

```mermaid
graph TD
    P["Publisher Client"] -->|"PUBLISH order:created payload"| S["Redis Server"]

    S -->|"채널 매칭"| CH["pubsub_channels dict"]
    S -->|"패턴 매칭"| PT["pubsub_patterns list"]

    CH -->|"order:created 채널"| C1["Subscriber A"]
    CH -->|"order:created 채널"| C2["Subscriber B"]
    PT -->|"order:* 패턴 매칭"| C3["Subscriber C (PSUBSCRIBE)"]

    subgraph "Keyspace Notification"
        KS["SET mykey value"] -->|"notify-keyspace-events 활성화"| KE["__keyevent__@0__:set"]
        KE --> C4["Subscriber D"]
    end

    style P fill:#fff3e0
    style S fill:#e1f5fe
    style C1 fill:#e8f5e9
    style C2 fill:#e8f5e9
    style C3 fill:#e8f5e9
    style C4 fill:#f3e5f5
```

### 3.2 채널 기반 구독: pubsub_channels dict

Redis 서버 내부에서 `pubsub_channels`는 딕셔너리(해시테이블)로 관리된다. 채널명이 키이고, 해당 채널을 구독 중인 클라이언트 리스트가 값이다.

```c
// Redis 서버 내부 구조 (server.h 기반)
struct redisServer {
    dict *pubsub_channels;    // 채널명 -> 구독 클라이언트 리스트
    list *pubsub_patterns;    // 패턴-클라이언트 매핑 리스트
    dict *pubsubshard_channels; // Redis 7+ 샤드 채널
};
```

`SUBSCRIBE` 명령이 실행되면:

1. `pubsub_channels` dict에서 채널명을 검색한다
2. 채널이 없으면 새로 생성하고, 클라이언트를 리스트에 추가한다
3. 이미 존재하면 기존 리스트에 클라이언트를 추가한다

`PUBLISH` 명령이 실행되면:

1. `pubsub_channels`에서 해당 채널을 찾아 모든 구독 클라이언트에게 메시지를 전송한다 (O(N))
2. `pubsub_patterns`를 순회하며 패턴이 일치하는 클라이언트에게도 전송한다 (O(N+M))

### 3.3 패턴 기반 구독: pubsub_patterns list

`PSUBSCRIBE` 명령은 glob 스타일 패턴을 사용하여 여러 채널을 한 번에 구독한다.

```bash
# 패턴 구독 예시
PSUBSCRIBE order:*           # order:created, order:updated 등 모두 수신
PSUBSCRIBE __keyevent@0__:* # 0번 DB의 모든 keyevent 수신
```

패턴 매칭은 `PUBLISH` 시마다 `pubsub_patterns` 전체를 순회하므로, 패턴이 많아지면 성능에 영향을 줄 수 있다. Redis 7.0에서는 이를 최적화하기 위해 내부적으로 패턴을 dict로 관리하는 개선이 적용되었다.

### 3.4 Keyspace Notification 설정

Keyspace Notification은 기본적으로 **비활성화** 상태이다. `notify-keyspace-events` 설정으로 활성화한다.

```bash
# redis.conf 또는 CONFIG SET으로 설정
CONFIG SET notify-keyspace-events Ex
```

| 문자 | 의미 |
|------|------|
| `K` | `__keyspace@<db>__` 접두사 이벤트 활성화 |
| `E` | `__keyevent@<db>__` 접두사 이벤트 활성화 |
| `g` | DEL, EXPIRE, RENAME 등 일반 명령 |
| `$` | String 명령 |
| `l` | List 명령 |
| `s` | Set 명령 |
| `h` | Hash 명령 |
| `z` | Sorted Set 명령 |
| `x` | 만료(expired) 이벤트 |
| `e` | 퇴출(evicted) 이벤트 |
| `A` | `g$lshzxe`의 별칭 (모든 이벤트) |

`Ex` 설정은 `__keyevent@<db>__:expired` 채널로 키 만료 이벤트를 발행한다.

### 3.5 메시지 전달 흐름

```mermaid
sequenceDiagram
    participant Pub as Publisher
    participant Redis as Redis Server
    participant Sub1 as Subscriber (SUBSCRIBE)
    participant Sub2 as Subscriber (PSUBSCRIBE)

    Sub1->>Redis: SUBSCRIBE order:created
    Redis-->>Sub1: subscribe 확인 (채널, 구독수)
    Sub2->>Redis: PSUBSCRIBE order:*
    Redis-->>Sub2: psubscribe 확인 (패턴, 구독수)

    Pub->>Redis: PUBLISH order:created '{"id":1}'

    Note over Redis: 1. pubsub_channels에서<br/>order:created 검색
    Redis-->>Sub1: message (order:created, '{"id":1}')

    Note over Redis: 2. pubsub_patterns 순회<br/>order:* 패턴 매칭
    Redis-->>Sub2: pmessage (order:*, order:created, '{"id":1}')

    Note over Redis: PUBLISH 반환값: 메시지를<br/>수신한 클라이언트 수 (2)
    Redis-->>Pub: (integer) 2
```

### 3.6 Pub/Sub vs Stream 비교

| 비교 항목 | Pub/Sub | Stream |
|----------|---------|--------|
| 메시지 보존 | 보존 안 됨 (Fire-and-forget) | 영구 보존 (XADD로 저장) |
| 전달 보장 | At-most-once | At-least-once (XACK 기반) |
| 구독자 부재 시 | 메시지 유실 | 이후 XREAD로 읽기 가능 |
| Consumer Group | 지원 안 함 | XGROUP으로 지원 |
| 메시지 되감기 | 불가 | XRANGE로 과거 메시지 조회 가능 |
| 오버헤드 | 낮음 (메모리 미사용) | 높음 (메시지 저장 필요) |
| 적합한 시나리오 | 실시간 알림, 캐시 무효화 | 작업 큐, 이벤트 소싱 |

## 4. 실전 예제

### 4.1 Spring Boot에서 Pub/Sub 메시지 발행/구독

```java
// Redis Pub/Sub 설정
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderEventSubscriber orderEventSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채널 기반 구독
        container.addMessageListener(
            orderEventSubscriber,
            new ChannelTopic("order:created")
        );

        // 패턴 기반 구독
        container.addMessageListener(
            orderEventSubscriber,
            new PatternTopic("order:*")
        );

        return container;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

```java
// 메시지 구독자 (Subscriber)
@Slf4j
@Component
public class OrderEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;

    public OrderEventSubscriber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("Received message on channel [{}]: {}", channel, body);

            OrderEvent event = objectMapper.readValue(body, OrderEvent.class);
            processOrderEvent(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message", e);
        }
    }

    private void processOrderEvent(OrderEvent event) {
        // 주문 이벤트 처리 로직
        log.info("Processing order event: orderId={}, status={}",
            event.getOrderId(), event.getStatus());
    }
}
```

```java
// 메시지 발행자 (Publisher)
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(
            order.getId(),
            "CREATED",
            LocalDateTime.now()
        );
        redisTemplate.convertAndSend("order:created", event);
    }

    public void publishOrderStatusChanged(Order order, String newStatus) {
        OrderEvent event = new OrderEvent(
            order.getId(),
            newStatus,
            LocalDateTime.now()
        );
        redisTemplate.convertAndSend("order:" + newStatus.toLowerCase(), event);
    }
}
```

### 4.2 Keyspace Notification으로 캐시 만료 감지

```java
// Keyspace Notification 설정 및 리스너
@Configuration
public class KeyspaceNotificationConfig {

    @Bean
    public RedisMessageListenerContainer keyspaceListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheExpirationHandler cacheExpirationHandler) {

        // Keyspace Notification 활성화 (Ex: expired 이벤트)
        RedisConnection connection = connectionFactory.getConnection();
        connection.serverCommands().setConfig(
            "notify-keyspace-events", "Ex"
        );
        connection.close();

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 0번 DB의 만료 이벤트 구독
        container.addMessageListener(
            cacheExpirationHandler,
            new PatternTopic("__keyevent@0__:expired")
        );

        return container;
    }
}
```

```java
// 캐시 만료 핸들러
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheExpirationHandler implements MessageListener {

    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        log.info("Key expired: {}", expiredKey);

        // 세션 키 만료 처리
        if (expiredKey.startsWith("session:")) {
            String sessionId = expiredKey.substring("session:".length());
            handleSessionExpired(sessionId);
        }

        // 캐시 키 만료 시 DB에서 재로드
        if (expiredKey.startsWith("cache:user:")) {
            String userId = expiredKey.substring("cache:user:".length());
            handleUserCacheExpired(userId);
        }
    }

    private void handleSessionExpired(String sessionId) {
        log.warn("Session expired: {}", sessionId);
        sessionRepository.markExpired(sessionId);
        notificationService.notifySessionExpired(sessionId);
    }

    private void handleUserCacheExpired(String userId) {
        log.info("User cache expired, scheduling reload: userId={}", userId);
        // 비동기로 캐시 재로드 예약
    }
}
```

### 4.3 Output Buffer 설정으로 느린 구독자 보호

```bash
# redis.conf - Pub/Sub 클라이언트의 output buffer 제한
# <class> <hard-limit> <soft-limit> <soft-seconds>
client-output-buffer-limit pubsub 256mb 64mb 60
```

위 설정은 Pub/Sub 구독자의 출력 버퍼가 256MB를 초과하면 즉시 연결을 끊고, 64MB를 60초 이상 유지하면 연결을 끊는다.

## 5. 정리

| 항목 | 설명 |
|-----|------|
| 채널 구독 | `SUBSCRIBE`로 특정 채널 구독, `pubsub_channels` dict에 저장 |
| 패턴 구독 | `PSUBSCRIBE`로 glob 패턴 구독, `pubsub_patterns` list에 저장 |
| 메시지 발행 | `PUBLISH`로 채널에 메시지 전송, 채널+패턴 모두 매칭하여 전달 |
| 전달 보장 | At-most-once, 구독자 부재 시 메시지 유실 |
| Keyspace Notification | `notify-keyspace-events` 설정으로 키 이벤트 구독 가능 |
| 만료 감지 | `Ex` 설정 후 `__keyevent@0__:expired` 채널 구독 |
| Stream과 차이 | Pub/Sub은 Fire-and-forget, Stream은 메시지 보존 및 Consumer Group 지원 |
| Output Buffer | `client-output-buffer-limit pubsub`으로 느린 구독자 보호 |

---
*참고: Redis 7.x 기준*
