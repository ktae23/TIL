# Quartz Scheduler

Spring에서 스케줄링 작업을 처리하기 위한 **Quartz Scheduler**의 핵심 개념과 사용법을 정리한다.

## Quartz란?

- Java 기반의 오픈소스 **작업 스케줄링 라이브러리**
- 복잡한 스케줄링 요구사항을 처리할 수 있음
- 클러스터링, 영속성, 트랜잭션 지원

## 핵심 구성 요소

### 1. Job

실제 실행할 작업을 정의하는 인터페이스

```java
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SampleJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Job 실행: " + System.currentTimeMillis());

        // JobDataMap에서 데이터 가져오기
        String param = context.getJobDetail()
                              .getJobDataMap()
                              .getString("param");
        System.out.println("파라미터: " + param);
    }
}
```

### 2. JobDetail

Job의 인스턴스를 정의하고 메타데이터를 설정

```java
import org.quartz.JobBuilder;
import org.quartz.JobDetail;

JobDetail jobDetail = JobBuilder.newJob(SampleJob.class)
    .withIdentity("sampleJob", "group1")      // 이름과 그룹
    .withDescription("샘플 작업입니다")
    .usingJobData("param", "hello")           // 파라미터 전달
    .storeDurably()                           // Trigger 없이도 유지
    .build();
```

### 3. Trigger

Job이 언제 실행될지 정의

#### SimpleTrigger - 단순 반복

```java
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger;

Trigger simpleTrigger = TriggerBuilder.newTrigger()
    .withIdentity("simpleTrigger", "group1")
    .startNow()
    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
        .withIntervalInSeconds(10)    // 10초마다
        .repeatForever())             // 무한 반복
    .build();
```

#### CronTrigger - Cron 표현식 사용

```java
import org.quartz.CronScheduleBuilder;

Trigger cronTrigger = TriggerBuilder.newTrigger()
    .withIdentity("cronTrigger", "group1")
    .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))  // 5분마다
    .build();
```

### 4. Scheduler

Job과 Trigger를 관리하고 실행

```java
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

SchedulerFactory factory = new StdSchedulerFactory();
Scheduler scheduler = factory.getScheduler();

// Job과 Trigger 등록
scheduler.scheduleJob(jobDetail, trigger);

// 스케줄러 시작
scheduler.start();

// 스케줄러 종료
// scheduler.shutdown();
```

## Spring Boot 연동

### 의존성 추가

```gradle
implementation 'org.springframework.boot:spring-boot-starter-quartz'
```

### Configuration 설정

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail sampleJobDetail() {
        return JobBuilder.newJob(SampleJob.class)
            .withIdentity("sampleJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger sampleTrigger(JobDetail sampleJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(sampleJobDetail)
            .withIdentity("sampleTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))  // 매분 실행
            .build();
    }
}
```

### application.yml 설정

```yaml
spring:
  quartz:
    job-store-type: jdbc          # 영속성 (memory | jdbc)
    jdbc:
      initialize-schema: always   # 스키마 자동 생성
    properties:
      org.quartz:
        scheduler:
          instanceName: MyScheduler
        threadPool:
          threadCount: 10
```

## Cron 표현식

| 필드 | 허용 값 | 특수문자 |
|------|---------|----------|
| 초 | 0-59 | , - * / |
| 분 | 0-59 | , - * / |
| 시 | 0-23 | , - * / |
| 일 | 1-31 | , - * ? / L W |
| 월 | 1-12 | , - * / |
| 요일 | 0-6 (0=일) | , - * ? / L # |

### 예시

```
0 0 12 * * ?      → 매일 12시 정각
0 0/30 * * * ?    → 30분마다
0 0 9 ? * MON-FRI → 평일 오전 9시
0 0 0 1 * ?       → 매월 1일 자정
```

## 요약

| 구성 요소 | 역할 |
|-----------|------|
| **Job** | 실행할 작업 로직 정의 |
| **JobDetail** | Job 인스턴스 + 메타데이터 |
| **Trigger** | 실행 시점/주기 정의 |
| **Scheduler** | Job + Trigger 관리 및 실행 |

*마지막 업데이트: 2026년 01월*
