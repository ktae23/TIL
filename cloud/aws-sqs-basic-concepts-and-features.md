# AWS SQS 기본 개념과 기능

AWS Simple Queue Service(SQS)는 완전 관리형 메시지 큐 서비스로, 분산 시스템 간의 비동기 통신을 가능하게 한다.

## 목차

- [SQS란?](#sqs란)
- [큐 유형](#큐-유형)
- [핵심 개념](#핵심-개념)
- [주요 옵션 및 설정](#주요-옵션-및-설정)
- [메시지 수명 주기](#메시지-수명-주기)
- [코드 예제](#코드-예제)
- [구현 원리](#구현-원리)

---

## SQS란?

SQS는 마이크로서비스, 분산 시스템, 서버리스 애플리케이션 간에 메시지를 안전하게 전송하고 저장하는 서비스다.

**주요 특징:**
- **완전 관리형**: 인프라 관리 불필요
- **무제한 처리량**: 초당 무제한 메시지 처리 가능
- **고가용성**: 여러 AZ에 걸쳐 메시지 중복 저장
- **보안**: 암호화 및 IAM 기반 접근 제어

---

## 큐 유형

### Standard Queue

```java
// Standard Queue 생성
CreateQueueRequest request = CreateQueueRequest.builder()
    .queueName("my-standard-queue")
    .build();

CreateQueueResponse response = sqsClient.createQueue(request);
```

| 특성 | 설명 |
|------|------|
| 처리량 | 무제한 TPS |
| 순서 보장 | Best-effort (순서 보장 안됨) |
| 중복 가능성 | At-least-once (중복 가능) |
| 사용 사례 | 높은 처리량이 필요한 작업 |

### FIFO Queue

```java
// FIFO Queue 생성 (이름이 .fifo로 끝나야 함)
Map<String, String> attributes = new HashMap<>();
attributes.put("FifoQueue", "true");
attributes.put("ContentBasedDeduplication", "true");

CreateQueueRequest request = CreateQueueRequest.builder()
    .queueName("my-fifo-queue.fifo")
    .attributes(attributes)
    .build();
```

| 특성 | 설명 |
|------|------|
| 처리량 | 300 TPS (배치 시 3,000) |
| 순서 보장 | 엄격한 FIFO 순서 보장 |
| 중복 가능성 | Exactly-once (중복 없음) |
| 사용 사례 | 순서가 중요한 금융 거래 등 |

---

## 핵심 개념

### Message

메시지는 최대 **256KB**까지 가능하며, 더 큰 데이터는 S3와 함께 사용한다.

```java
// 메시지 전송
SendMessageRequest sendRequest = SendMessageRequest.builder()
    .queueUrl(queueUrl)
    .messageBody("Hello SQS!")
    .delaySeconds(10)  // 지연 전송
    .messageAttributes(Map.of(
        "Author", MessageAttributeValue.builder()
            .stringValue("John")
            .dataType("String")
            .build()
    ))
    .build();

sqsClient.sendMessage(sendRequest);
```

### Message Group ID (FIFO 전용)

같은 그룹 내 메시지는 순서대로 처리된다.

```java
// FIFO 큐에 메시지 전송
SendMessageRequest fifoRequest = SendMessageRequest.builder()
    .queueUrl(fifoQueueUrl)
    .messageBody("Order #12345")
    .messageGroupId("order-processing")  // 필수
    .messageDeduplicationId("unique-id-12345")  // 또는 ContentBasedDeduplication 사용
    .build();
```

### Dead Letter Queue (DLQ)

처리 실패한 메시지를 보관하는 큐다.

```java
// DLQ 설정이 포함된 큐 생성
String dlqArn = "arn:aws:sqs:ap-northeast-2:123456789:my-dlq";

Map<String, String> attributes = new HashMap<>();
attributes.put("RedrivePolicy", String.format(
    "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"3\"}",
    dlqArn
));

CreateQueueRequest request = CreateQueueRequest.builder()
    .queueName("my-queue-with-dlq")
    .attributes(attributes)
    .build();
```

---

## 주요 옵션 및 설정

### Visibility Timeout

메시지를 수신한 후 다른 소비자에게 보이지 않는 시간이다.

```java
// 큐 레벨에서 설정 (기본값: 30초, 최대: 12시간)
Map<String, String> attributes = new HashMap<>();
attributes.put("VisibilityTimeout", "60");  // 60초

// 메시지 수신 시 개별 설정
ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    .queueUrl(queueUrl)
    .visibilityTimeout(120)  // 이 요청에서만 120초 적용
    .maxNumberOfMessages(10)
    .build();

// 처리 중 시간 연장
ChangeMessageVisibilityRequest changeRequest = ChangeMessageVisibilityRequest.builder()
    .queueUrl(queueUrl)
    .receiptHandle(receiptHandle)
    .visibilityTimeout(300)  // 5분으로 연장
    .build();

sqsClient.changeMessageVisibility(changeRequest);
```

### Message Retention Period

메시지가 큐에 보관되는 최대 기간이다.

```java
// 보관 기간 설정 (기본: 4일, 범위: 1분 ~ 14일)
Map<String, String> attributes = new HashMap<>();
attributes.put("MessageRetentionPeriod", "1209600");  // 14일 (초 단위)
```

### Delay Queue

메시지 전송 후 소비자에게 보이기까지의 지연 시간이다.

```java
// 큐 레벨 지연 설정 (최대: 15분)
Map<String, String> attributes = new HashMap<>();
attributes.put("DelaySeconds", "300");  // 5분 지연

// 개별 메시지 지연 (큐 설정보다 우선)
SendMessageRequest request = SendMessageRequest.builder()
    .queueUrl(queueUrl)
    .messageBody("Delayed message")
    .delaySeconds(600)  // 10분 지연
    .build();
```

### Long Polling

빈 응답을 줄이고 비용을 절감하는 방식이다.

```java
// Long Polling 활성화 (최대: 20초)
ReceiveMessageRequest request = ReceiveMessageRequest.builder()
    .queueUrl(queueUrl)
    .waitTimeSeconds(20)  // 메시지가 올 때까지 최대 20초 대기
    .maxNumberOfMessages(10)
    .build();

List<Message> messages = sqsClient.receiveMessage(request).messages();
```

| Polling 방식 | 특징 |
|-------------|------|
| Short Polling | 즉시 응답, 빈 응답 많음, 비용 높음 |
| Long Polling | 대기 후 응답, 빈 응답 적음, 비용 절감 |

---

## 메시지 수명 주기

```
Producer                    SQS Queue                     Consumer
   |                           |                             |
   |-- SendMessage ----------->|                             |
   |                           |                             |
   |                           |<-------- ReceiveMessage ----|
   |                           |                             |
   |                     [Visibility Timeout 시작]           |
   |                           |                             |
   |                           |                      [메시지 처리]
   |                           |                             |
   |                           |<-------- DeleteMessage -----|
   |                           |                             |
   |                     [메시지 삭제됨]                      |
```

### 전체 흐름 코드

```java
public class SQSMessageProcessor {
    private final SqsClient sqsClient;
    private final String queueUrl;

    public void processMessages() {
        // 1. 메시지 수신 (Long Polling)
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(20)
            .attributeNamesWithStrings("All")
            .messageAttributeNames("All")
            .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        for (Message message : messages) {
            try {
                // 2. 메시지 처리
                processMessage(message);

                // 3. 처리 성공 시 삭제
                DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

                sqsClient.deleteMessage(deleteRequest);

            } catch (Exception e) {
                // 처리 실패 시 Visibility Timeout 후 재시도됨
                // maxReceiveCount 초과 시 DLQ로 이동
                log.error("Failed to process message: {}", message.messageId(), e);
            }
        }
    }

    private void processMessage(Message message) {
        // 비즈니스 로직
        System.out.println("Processing: " + message.body());
    }
}
```

---

## 코드 예제

### Python (boto3)

```python
import boto3
import json

# SQS 클라이언트 생성
sqs = boto3.client('sqs', region_name='ap-northeast-2')
queue_url = 'https://sqs.ap-northeast-2.amazonaws.com/123456789/my-queue'

# 메시지 전송
def send_message(body: dict):
    response = sqs.send_message(
        QueueUrl=queue_url,
        MessageBody=json.dumps(body),
        MessageAttributes={
            'ContentType': {
                'DataType': 'String',
                'StringValue': 'application/json'
            }
        }
    )
    return response['MessageId']

# 메시지 수신 및 처리
def process_messages():
    while True:
        response = sqs.receive_message(
            QueueUrl=queue_url,
            MaxNumberOfMessages=10,
            WaitTimeSeconds=20,
            MessageAttributeNames=['All']
        )

        messages = response.get('Messages', [])

        for message in messages:
            try:
                body = json.loads(message['Body'])
                print(f"Processing: {body}")

                # 처리 성공 시 삭제
                sqs.delete_message(
                    QueueUrl=queue_url,
                    ReceiptHandle=message['ReceiptHandle']
                )
            except Exception as e:
                print(f"Error: {e}")

# 배치 전송 (비용 절감)
def send_batch(messages: list):
    entries = [
        {
            'Id': str(i),
            'MessageBody': json.dumps(msg)
        }
        for i, msg in enumerate(messages)
    ]

    response = sqs.send_message_batch(
        QueueUrl=queue_url,
        Entries=entries
    )
    return response
```

### Spring Boot 연동

```java
// build.gradle
// implementation 'io.awspring.cloud:spring-cloud-aws-messaging:2.4.4'

// application.yml
// cloud:
//   aws:
//     sqs:
//       endpoint: https://sqs.ap-northeast-2.amazonaws.com
//     region:
//       static: ap-northeast-2

@Service
@RequiredArgsConstructor
public class OrderEventHandler {

    private final QueueMessagingTemplate queueMessagingTemplate;

    // 메시지 전송
    public void sendOrderEvent(OrderEvent event) {
        queueMessagingTemplate.convertAndSend("order-queue", event);
    }

    // 메시지 수신 (리스너)
    @SqsListener(value = "order-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void handleOrderEvent(@Payload OrderEvent event, @Headers Map<String, String> headers) {
        log.info("Received order event: {}", event.getOrderId());

        // 비즈니스 로직 처리
        processOrder(event);

        // 예외 발생 시 메시지가 삭제되지 않고 재처리됨
    }
}
```

---

## 구현 원리

### 분산 저장

SQS는 메시지를 여러 서버와 데이터센터에 중복 저장한다.

```
                    ┌─────────────────┐
                    │   SQS Service   │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼────┐        ┌────▼────┐        ┌────▼────┐
    │ Server1 │        │ Server2 │        │ Server3 │
    │  (AZ-a) │        │  (AZ-b) │        │  (AZ-c) │
    └─────────┘        └─────────┘        └─────────┘
```

### Visibility Timeout 동작 원리

```
시간 →
─────────────────────────────────────────────────────────────►

│◄──── 메시지 보임 ────►│◄── Visibility Timeout ──►│◄─ 다시 보임 ─►│
                        │                          │
                   Consumer A                 Consumer B
                    수신 시점              재처리 가능 시점
```

### FIFO 순서 보장 원리

```
Message Group "order-A":  M1 → M2 → M3  (순서대로 처리)
Message Group "order-B":  M4 → M5      (별도로 순서 처리)
                          ↓
                     병렬 처리 가능
                          ↓
Consumer 1: order-A 그룹 처리
Consumer 2: order-B 그룹 처리
```

---

## 모범 사례

1. **적절한 Visibility Timeout 설정**: 처리 시간의 6배 정도로 설정
2. **Long Polling 사용**: 비용 절감 및 효율성 향상
3. **DLQ 반드시 설정**: 실패 메시지 추적 및 분석
4. **배치 작업 활용**: 비용 및 처리량 최적화
5. **멱등성 보장**: Standard Queue의 중복 메시지 대비

```java
// 멱등성 보장 예시
@Transactional
public void processOrder(OrderEvent event) {
    // 이미 처리된 메시지인지 확인
    if (processedMessageRepository.existsByMessageId(event.getMessageId())) {
        log.info("Already processed: {}", event.getMessageId());
        return;
    }

    // 비즈니스 로직 처리
    orderService.createOrder(event);

    // 처리 완료 기록
    processedMessageRepository.save(new ProcessedMessage(event.getMessageId()));
}
```

---

*마지막 업데이트: 2025년 12월*
