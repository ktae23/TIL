# Java 메트릭 모니터링 및 Virtual Thread Pin 감지

Java 애플리케이션의 메트릭 수집, Virtual Thread 모니터링, 그리고 Pinning 감지 방법을 정리한다.

## 목차

- [JVM 메트릭 모니터링 기초](#jvm-메트릭-모니터링-기초)
- [Micrometer를 이용한 메트릭 수집](#micrometer를-이용한-메트릭-수집)
- [Virtual Thread 모니터링](#virtual-thread-모니터링)
- [Virtual Thread Pinning 감지](#virtual-thread-pinning-감지)
- [MDC와 Virtual Thread](#mdc와-virtual-thread)
- [Prometheus + Grafana 연동](#prometheus--grafana-연동)
- [실전 모니터링 대시보드](#실전-모니터링-대시보드)

---

## JVM 메트릭 모니터링 기초

### MXBean을 이용한 기본 메트릭 수집

```java
import java.lang.management.*;

public class JvmMetricsCollector {

    public static void collectBasicMetrics() {
        // 메모리 메트릭
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        System.out.println("=== Memory Metrics ===");
        System.out.printf("Heap Used: %d MB%n", heapUsage.getUsed() / 1024 / 1024);
        System.out.printf("Heap Max: %d MB%n", heapUsage.getMax() / 1024 / 1024);
        System.out.printf("Heap Usage: %.2f%%%n",
            (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        System.out.printf("Non-Heap Used: %d MB%n", nonHeapUsage.getUsed() / 1024 / 1024);

        // GC 메트릭
        System.out.println("\n=== GC Metrics ===");
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("%s - Count: %d, Time: %d ms%n",
                gcBean.getName(),
                gcBean.getCollectionCount(),
                gcBean.getCollectionTime());
        }

        // 스레드 메트릭
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        System.out.println("\n=== Thread Metrics ===");
        System.out.printf("Thread Count: %d%n", threadBean.getThreadCount());
        System.out.printf("Peak Thread Count: %d%n", threadBean.getPeakThreadCount());
        System.out.printf("Daemon Thread Count: %d%n", threadBean.getDaemonThreadCount());

        // CPU 메트릭
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        System.out.println("\n=== CPU Metrics ===");
        System.out.printf("Available Processors: %d%n", osBean.getAvailableProcessors());
        System.out.printf("System Load Average: %.2f%n", osBean.getSystemLoadAverage());

        // 상세 CPU 메트릭 (com.sun.management)
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            System.out.printf("Process CPU Load: %.2f%%%n",
                sunOsBean.getProcessCpuLoad() * 100);
            System.out.printf("System CPU Load: %.2f%%%n",
                sunOsBean.getCpuLoad() * 100);
        }
    }

    // 메모리 풀별 상세 메트릭
    public static void collectMemoryPoolMetrics() {
        System.out.println("\n=== Memory Pool Metrics ===");
        for (MemoryPoolMXBean poolBean : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = poolBean.getUsage();
            System.out.printf("%s (%s):%n", poolBean.getName(), poolBean.getType());
            System.out.printf("  Used: %d KB, Max: %d KB%n",
                usage.getUsed() / 1024,
                usage.getMax() > 0 ? usage.getMax() / 1024 : -1);
        }
    }
}
```

### JFR (Java Flight Recorder) 이벤트 수집

```java
import jdk.jfr.*;
import jdk.jfr.consumer.*;
import java.time.Duration;
import java.nio.file.Path;

public class JfrMetricsCollector {

    // 커스텀 JFR 이벤트 정의
    @Name("com.example.HttpRequest")
    @Label("HTTP Request")
    @Category({"Application", "HTTP"})
    @Description("HTTP request handling event")
    static class HttpRequestEvent extends Event {
        @Label("Method")
        String method;

        @Label("Path")
        String path;

        @Label("Status Code")
        int statusCode;

        @Label("Duration (ms)")
        long durationMs;
    }

    // 이벤트 기록
    public void handleRequest(String method, String path) {
        HttpRequestEvent event = new HttpRequestEvent();
        event.begin();

        try {
            // 요청 처리 로직
            int status = processRequest(method, path);
            event.method = method;
            event.path = path;
            event.statusCode = status;
        } finally {
            event.end();
            event.durationMs = event.getDuration().toMillis();
            event.commit();
        }
    }

    // JFR 스트리밍으로 실시간 메트릭 수집
    public static void startJfrStreaming() {
        try (RecordingStream rs = new RecordingStream()) {
            // CPU 로드 이벤트 수집
            rs.enable("jdk.CPULoad")
                .withPeriod(Duration.ofSeconds(1));

            // GC 이벤트 수집
            rs.enable("jdk.GCHeapSummary");
            rs.enable("jdk.GarbageCollection");

            // Virtual Thread 관련 이벤트
            rs.enable("jdk.VirtualThreadStart");
            rs.enable("jdk.VirtualThreadEnd");
            rs.enable("jdk.VirtualThreadPinned");

            // 이벤트 핸들러 등록
            rs.onEvent("jdk.CPULoad", event -> {
                System.out.printf("CPU - JVM: %.2f%%, System: %.2f%%, Machine: %.2f%%%n",
                    event.getFloat("jvmUser") * 100,
                    event.getFloat("jvmSystem") * 100,
                    event.getFloat("machineTotal") * 100);
            });

            rs.onEvent("jdk.GarbageCollection", event -> {
                System.out.printf("GC - Name: %s, Cause: %s, Duration: %d ms%n",
                    event.getString("name"),
                    event.getString("cause"),
                    event.getDuration().toMillis());
            });

            rs.onEvent("jdk.VirtualThreadPinned", event -> {
                System.out.printf("⚠️ Virtual Thread Pinned! Duration: %d ms%n",
                    event.getDuration().toMillis());
                RecordedStackTrace stackTrace = event.getStackTrace();
                if (stackTrace != null) {
                    stackTrace.getFrames().stream()
                        .limit(10)
                        .forEach(frame -> System.out.println("  at " + frame));
                }
            });

            rs.startAsync();

            // 계속 실행
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // JFR 파일로 기록
    public static void recordToFile(Duration duration, Path outputPath) throws Exception {
        Configuration config = Configuration.getConfiguration("profile");

        try (Recording recording = new Recording(config)) {
            recording.setDestination(outputPath);
            recording.setDuration(duration);

            // Virtual Thread 이벤트 활성화
            recording.enable("jdk.VirtualThreadStart");
            recording.enable("jdk.VirtualThreadEnd");
            recording.enable("jdk.VirtualThreadPinned").withStackTrace();
            recording.enable("jdk.VirtualThreadSubmitFailed");

            recording.start();
            Thread.sleep(duration.toMillis());
        }
    }
}
```

---

## Micrometer를 이용한 메트릭 수집

### 기본 설정 (Spring Boot)

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

```java
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // JVM 메트릭 바인더 등록
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new JvmHeapPressureMetrics().bindTo(registry);
        new JvmInfoMetrics().bindTo(registry);

        // 시스템 메트릭
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new FileDescriptorMetrics().bindTo(registry);

        // 커스텀 태그 추가
        registry.config().commonTags(
            "application", "my-app",
            "environment", "production"
        );

        return registry;
    }
}
```

### 커스텀 메트릭 정의

```java
@Component
public class CustomMetrics {

    private final Counter requestCounter;
    private final Timer requestTimer;
    private final Gauge activeVirtualThreads;
    private final DistributionSummary requestSizeDistribution;
    private final AtomicInteger activeVThreadCount = new AtomicInteger(0);

    public CustomMetrics(MeterRegistry registry) {
        // Counter - 요청 횟수
        this.requestCounter = Counter.builder("http_requests_total")
            .description("Total HTTP requests")
            .tags("service", "api")
            .register(registry);

        // Timer - 응답 시간
        this.requestTimer = Timer.builder("http_request_duration")
            .description("HTTP request duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .serviceLevelObjectives(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofSeconds(1)
            )
            .register(registry);

        // Gauge - 현재 활성 Virtual Thread 수
        this.activeVirtualThreads = Gauge.builder("virtual_threads_active", activeVThreadCount, AtomicInteger::get)
            .description("Number of active virtual threads")
            .register(registry);

        // Distribution Summary - 요청 크기 분포
        this.requestSizeDistribution = DistributionSummary.builder("http_request_size")
            .description("HTTP request body size")
            .baseUnit("bytes")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    // 요청 처리 시 메트릭 기록
    public <T> T recordRequest(String method, String path, Supplier<T> action) {
        requestCounter.increment();

        return requestTimer.record(() -> {
            Tags tags = Tags.of("method", method, "path", path);
            return Timer.builder("http_request_duration_detailed")
                .tags(tags)
                .register(Metrics.globalRegistry)
                .record(action);
        });
    }

    // Virtual Thread 시작/종료 시 카운트
    public void virtualThreadStarted() {
        activeVThreadCount.incrementAndGet();
    }

    public void virtualThreadEnded() {
        activeVThreadCount.decrementAndGet();
    }

    public void recordRequestSize(long bytes) {
        requestSizeDistribution.record(bytes);
    }
}
```

### Virtual Thread 전용 메트릭

```java
@Component
public class VirtualThreadMetrics implements MeterBinder {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong pinnedCount = new AtomicLong(0);
    private final AtomicLong totalVirtualThreads = new AtomicLong(0);
    private final AtomicLong mountedVirtualThreads = new AtomicLong(0);

    @Override
    public void bindTo(MeterRegistry registry) {
        // Virtual Thread 생성 수
        Gauge.builder("jvm_virtual_threads_total", totalVirtualThreads, AtomicLong::get)
            .description("Total number of virtual threads created")
            .register(registry);

        // 현재 마운트된 Virtual Thread 수
        Gauge.builder("jvm_virtual_threads_mounted", mountedVirtualThreads, AtomicLong::get)
            .description("Number of currently mounted virtual threads")
            .register(registry);

        // Pinned 이벤트 수
        Gauge.builder("jvm_virtual_threads_pinned_total", pinnedCount, AtomicLong::get)
            .description("Total number of virtual thread pinning events")
            .register(registry);

        // Carrier Thread 수 (Platform Thread)
        Gauge.builder("jvm_carrier_threads", this, m -> getCarrierThreadCount())
            .description("Number of carrier threads")
            .register(registry);

        // JFR 이벤트 기반 메트릭 수집 시작
        startJfrMonitoring();
    }

    private void startJfrMonitoring() {
        try {
            RecordingStream rs = new RecordingStream();

            rs.enable("jdk.VirtualThreadStart");
            rs.enable("jdk.VirtualThreadEnd");
            rs.enable("jdk.VirtualThreadPinned").withStackTrace();

            rs.onEvent("jdk.VirtualThreadStart", event -> {
                totalVirtualThreads.incrementAndGet();
                mountedVirtualThreads.incrementAndGet();
            });

            rs.onEvent("jdk.VirtualThreadEnd", event -> {
                mountedVirtualThreads.decrementAndGet();
            });

            rs.onEvent("jdk.VirtualThreadPinned", event -> {
                pinnedCount.incrementAndGet();
            });

            rs.startAsync();
        } catch (Exception e) {
            // JFR not available
        }
    }

    private double getCarrierThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().startsWith("ForkJoinPool") &&
                        t.getName().contains("worker"))
            .count();
    }
}
```

---

## Virtual Thread 모니터링

### Virtual Thread 상태 추적

```java
public class VirtualThreadMonitor {

    private static final ConcurrentHashMap<Long, VirtualThreadInfo> activeThreads =
        new ConcurrentHashMap<>();

    record VirtualThreadInfo(
        long id,
        String name,
        Instant startTime,
        Thread.State state,
        StackTraceElement[] stackTrace
    ) {}

    // Virtual Thread 래퍼 - 모니터링 포함
    public static Thread startMonitoredVirtualThread(String name, Runnable task) {
        return Thread.ofVirtual()
            .name(name)
            .start(() -> {
                Thread current = Thread.currentThread();
                long id = current.threadId();

                // 시작 기록
                activeThreads.put(id, new VirtualThreadInfo(
                    id,
                    name,
                    Instant.now(),
                    Thread.State.RUNNABLE,
                    null
                ));

                try {
                    task.run();
                } finally {
                    // 종료 기록
                    activeThreads.remove(id);
                }
            });
    }

    // 현재 활성 Virtual Thread 상태 조회
    public static void printActiveVirtualThreads() {
        System.out.println("=== Active Virtual Threads ===");
        System.out.printf("Total: %d%n%n", activeThreads.size());

        activeThreads.forEach((id, info) -> {
            Duration elapsed = Duration.between(info.startTime(), Instant.now());
            System.out.printf("Thread[%d] %s - Running for %d ms%n",
                id, info.name(), elapsed.toMillis());
        });
    }

    // 스레드 덤프 수집
    public static void collectVirtualThreadDump() {
        System.out.println("=== Virtual Thread Dump ===");

        Thread.getAllStackTraces().forEach((thread, stackTrace) -> {
            if (thread.isVirtual()) {
                System.out.printf("%nVirtual Thread[%d]: %s (%s)%n",
                    thread.threadId(),
                    thread.getName(),
                    thread.getState());

                for (StackTraceElement element : stackTrace) {
                    System.out.println("    at " + element);
                }
            }
        });
    }
}
```

### ExecutorService 메트릭 수집

```java
public class MonitoredVirtualThreadExecutor implements AutoCloseable {

    private final ExecutorService executor;
    private final MeterRegistry registry;
    private final String name;

    private final AtomicLong submittedTasks = new AtomicLong(0);
    private final AtomicLong completedTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public MonitoredVirtualThreadExecutor(String name, MeterRegistry registry) {
        this.name = name;
        this.registry = registry;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        // 메트릭 등록
        Gauge.builder("executor_active_tasks", activeTasks, AtomicInteger::get)
            .tag("executor", name)
            .description("Number of currently active tasks")
            .register(registry);

        Counter.builder("executor_submitted_tasks_total")
            .tag("executor", name)
            .description("Total submitted tasks")
            .register(registry);

        Counter.builder("executor_completed_tasks_total")
            .tag("executor", name)
            .description("Total completed tasks")
            .register(registry);

        Counter.builder("executor_failed_tasks_total")
            .tag("executor", name)
            .description("Total failed tasks")
            .register(registry);
    }

    public <T> Future<T> submit(Callable<T> task) {
        submittedTasks.incrementAndGet();
        registry.counter("executor_submitted_tasks_total", "executor", name).increment();

        return executor.submit(() -> {
            activeTasks.incrementAndGet();
            Timer.Sample sample = Timer.start(registry);

            try {
                T result = task.call();
                completedTasks.incrementAndGet();
                registry.counter("executor_completed_tasks_total", "executor", name).increment();
                return result;
            } catch (Exception e) {
                failedTasks.incrementAndGet();
                registry.counter("executor_failed_tasks_total", "executor", name).increment();
                throw e;
            } finally {
                activeTasks.decrementAndGet();
                sample.stop(Timer.builder("executor_task_duration")
                    .tag("executor", name)
                    .register(registry));
            }
        });
    }

    public void execute(Runnable task) {
        submit(() -> {
            task.run();
            return null;
        });
    }

    @Override
    public void close() {
        executor.close();
    }

    // 상태 출력
    public void printStats() {
        System.out.printf("Executor[%s] - Submitted: %d, Completed: %d, Failed: %d, Active: %d%n",
            name,
            submittedTasks.get(),
            completedTasks.get(),
            failedTasks.get(),
            activeTasks.get());
    }
}
```

---

## Virtual Thread Pinning 감지

Pinning은 Virtual Thread가 Carrier Thread에 고정되어 다른 Virtual Thread를 스케줄링할 수 없는 상태이다.

### Pinning이 발생하는 경우

| 원인 | 설명 | 해결 방법 |
|------|------|----------|
| `synchronized` 블록 | 모니터 락 획득 시 | `ReentrantLock` 사용 |
| `synchronized` 메서드 | 모니터 락 획득 시 | `ReentrantLock` 사용 |
| 네이티브 메서드 (JNI) | 네이티브 코드 실행 중 | 피할 수 없음, 짧게 유지 |
| `Object.wait()` | 모니터 대기 시 | `Condition.await()` 사용 |

### JVM 옵션으로 Pinning 감지

```bash
# Pinning 발생 시 스택 트레이스 출력
java -Djdk.tracePinnedThreads=full MyApp

# Pinning 발생 시 간단한 메시지만 출력
java -Djdk.tracePinnedThreads=short MyApp
```

### JFR을 이용한 Pinning 감지

```java
public class PinningDetector {

    private static final List<PinningEvent> pinningEvents =
        Collections.synchronizedList(new ArrayList<>());

    record PinningEvent(
        Instant timestamp,
        Duration duration,
        String threadName,
        List<String> stackTrace
    ) {}

    // JFR 스트리밍으로 실시간 감지
    public static void startPinningDetection() {
        try (RecordingStream rs = new RecordingStream()) {
            // Pinning 이벤트 활성화 (스택 트레이스 포함)
            rs.enable("jdk.VirtualThreadPinned")
                .withStackTrace()
                .withThreshold(Duration.ofMillis(1));  // 1ms 이상만

            rs.onEvent("jdk.VirtualThreadPinned", event -> {
                Duration duration = event.getDuration();
                RecordedStackTrace stackTrace = event.getStackTrace();

                List<String> frames = new ArrayList<>();
                if (stackTrace != null) {
                    stackTrace.getFrames().stream()
                        .limit(20)
                        .forEach(frame -> frames.add(formatFrame(frame)));
                }

                PinningEvent pe = new PinningEvent(
                    event.getStartTime(),
                    duration,
                    event.getThread().getJavaName(),
                    frames
                );

                pinningEvents.add(pe);
                logPinningEvent(pe);
            });

            rs.startAsync();

            System.out.println("Pinning detection started. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatFrame(RecordedFrame frame) {
        return String.format("%s.%s(%s:%d)",
            frame.getMethod().getType().getName(),
            frame.getMethod().getName(),
            frame.getMethod().getType().getName().substring(
                frame.getMethod().getType().getName().lastIndexOf('.') + 1) + ".java",
            frame.getLineNumber());
    }

    private static void logPinningEvent(PinningEvent event) {
        System.out.println("\n⚠️ === PINNING DETECTED ===");
        System.out.printf("Time: %s%n", event.timestamp());
        System.out.printf("Duration: %d ms%n", event.duration().toMillis());
        System.out.printf("Thread: %s%n", event.threadName());
        System.out.println("Stack Trace:");
        event.stackTrace().forEach(frame -> System.out.println("  at " + frame));
        System.out.println("===========================\n");
    }

    // Pinning 통계 출력
    public static void printPinningStats() {
        System.out.println("\n=== Pinning Statistics ===");
        System.out.printf("Total Events: %d%n", pinningEvents.size());

        if (!pinningEvents.isEmpty()) {
            DoubleSummaryStatistics stats = pinningEvents.stream()
                .mapToDouble(e -> e.duration().toMillis())
                .summaryStatistics();

            System.out.printf("Average Duration: %.2f ms%n", stats.getAverage());
            System.out.printf("Max Duration: %.2f ms%n", stats.getMax());
            System.out.printf("Min Duration: %.2f ms%n", stats.getMin());

            // 가장 빈번한 Pinning 위치
            System.out.println("\nTop Pinning Locations:");
            pinningEvents.stream()
                .filter(e -> !e.stackTrace().isEmpty())
                .collect(Collectors.groupingBy(
                    e -> e.stackTrace().get(0),
                    Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry ->
                    System.out.printf("  %d times: %s%n", entry.getValue(), entry.getKey()));
        }
    }
}
```

### Pinning 방지 코드 패턴

```java
public class PinningAvoidance {

    // ❌ Bad: synchronized 사용 - Pinning 발생
    private final Object lock = new Object();
    private int counter = 0;

    public void incrementBad() {
        synchronized (lock) {
            // Virtual Thread가 여기서 pinning됨
            counter++;
            try {
                Thread.sleep(100);  // I/O 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ✅ Good: ReentrantLock 사용 - Pinning 없음
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public void incrementGood() {
        reentrantLock.lock();
        try {
            counter++;
            try {
                Thread.sleep(100);  // I/O 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    // ❌ Bad: Object.wait() 사용
    public void waitBad() throws InterruptedException {
        synchronized (lock) {
            lock.wait();  // Pinning 발생
        }
    }

    // ✅ Good: Condition.await() 사용
    private final Condition condition = reentrantLock.newCondition();

    public void waitGood() throws InterruptedException {
        reentrantLock.lock();
        try {
            condition.await();  // Pinning 없음
        } finally {
            reentrantLock.unlock();
        }
    }

    // ✅ Good: 동기화가 짧은 경우는 synchronized도 괜찮음
    public void quickSyncOk() {
        synchronized (lock) {
            counter++;  // 매우 빠른 연산만
        }
        // I/O 작업은 synchronized 밖에서
        doIoOperation();
    }
}
```

### 자동 Pinning 감지 테스트

```java
@ExtendWith(PinningDetectorExtension.class)
public class VirtualThreadTest {

    @Test
    @DetectPinning(maxAllowed = 0)  // Pinning 발생 시 테스트 실패
    void shouldNotCausePinning() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = IntStream.range(0, 100)
                .mapToObj(i -> executor.submit(() -> {
                    // 테스트할 코드
                    processWithLock();
                    return i;
                }))
                .toList();

            futures.forEach(f -> {
                try {
                    f.get();
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }
}

// JUnit Extension으로 Pinning 감지
public class PinningDetectorExtension implements BeforeEachCallback, AfterEachCallback {

    private RecordingStream recordingStream;
    private final AtomicInteger pinningCount = new AtomicInteger(0);

    @Override
    public void beforeEach(ExtensionContext context) {
        pinningCount.set(0);
        recordingStream = new RecordingStream();
        recordingStream.enable("jdk.VirtualThreadPinned").withStackTrace();
        recordingStream.onEvent("jdk.VirtualThreadPinned", event -> {
            pinningCount.incrementAndGet();
            System.out.println("Pinning detected in test!");
        });
        recordingStream.startAsync();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        recordingStream.close();

        context.getTestMethod().ifPresent(method -> {
            DetectPinning annotation = method.getAnnotation(DetectPinning.class);
            if (annotation != null && pinningCount.get() > annotation.maxAllowed()) {
                throw new AssertionError(
                    String.format("Pinning detected %d times (max allowed: %d)",
                        pinningCount.get(), annotation.maxAllowed()));
            }
        });
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DetectPinning {
    int maxAllowed() default 0;
}
```

---

## MDC와 Virtual Thread

### MDC (Mapped Diagnostic Context)란?

MDC는 로깅 프레임워크(SLF4J, Logback, Log4j)에서 제공하는 **스레드별 컨텍스트 저장소**이다. 요청 ID, 사용자 ID 등을 저장하면 해당 스레드의 모든 로그에 자동으로 포함된다.

```java
import org.slf4j.MDC;

// 요청 시작 시 컨텍스트 설정
MDC.put("requestId", UUID.randomUUID().toString());
MDC.put("userId", "user123");
MDC.put("traceId", "abc-123-xyz");

// 이후 모든 로그에 자동으로 포함됨
log.info("주문 처리 시작");  // [requestId=abc-123, userId=user123] 주문 처리 시작
log.info("결제 완료");       // [requestId=abc-123, userId=user123] 결제 완료

// 요청 종료 시 정리
MDC.clear();
```

### Logback 패턴 설정

```xml
<!-- logback.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] [%X{requestId}] [%X{userId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON 포맷 (ELK 스택용) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>traceId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### MDC 주요 용도

| 용도 | MDC Key 예시 | 설명 |
|------|-------------|------|
| **요청 추적** | requestId, correlationId | 단일 요청의 전체 흐름 추적 |
| **분산 트레이싱** | traceId, spanId | MSA 환경에서 서비스 간 추적 |
| **사용자 식별** | userId, sessionId | 사용자별 로그 필터링 |
| **멀티테넌시** | tenantId | 테넌트별 로그 분리 |
| **성능 분석** | startTime | 요청 처리 시간 계산 |

### Virtual Thread에서 MDC 문제점

MDC는 내부적으로 **ThreadLocal**을 사용한다. Virtual Thread는 Carrier Thread 위에서 실행되며, 새 Virtual Thread를 생성하면 MDC 컨텍스트가 전파되지 않는다.

```java
// ❌ 문제: Virtual Thread 간 MDC 전파 안 됨
MDC.put("requestId", "req-123");
log.info("Main: {}", MDC.get("requestId"));  // Main: req-123

Thread.startVirtualThread(() -> {
    log.info("Virtual: {}", MDC.get("requestId"));  // Virtual: null ❌
});

// ❌ ExecutorService도 동일한 문제
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    MDC.put("requestId", "req-456");

    executor.submit(() -> {
        log.info("Task: {}", MDC.get("requestId"));  // Task: null ❌
    });
}
```

### 해결 방법 1: 수동 MDC 전파

```java
public class MdcVirtualThreadUtils {

    // MDC 컨텍스트를 복사하여 Virtual Thread 실행
    public static Thread startVirtualThreadWithMdc(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return Thread.startVirtualThread(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        });
    }

    // Callable 버전
    public static <T> Callable<T> wrapWithMdc(Callable<T> task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        };
    }

    // Runnable 버전
    public static Runnable wrapWithMdc(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

// 사용 예시
MDC.put("requestId", "req-123");

MdcVirtualThreadUtils.startVirtualThreadWithMdc(() -> {
    log.info("Virtual: {}", MDC.get("requestId"));  // Virtual: req-123 ✅
});
```

### 해결 방법 2: MDC 전파 ExecutorService 래퍼

```java
public class MdcAwareVirtualThreadExecutor implements ExecutorService {

    private final ExecutorService delegate;

    public MdcAwareVirtualThreadExecutor() {
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrapWithMdc(command));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapWithMdc(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrapWithMdc(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrapWithMdc(task), result);
    }

    private Runnable wrapWithMdc(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }

    private <T> Callable<T> wrapWithMdc(Callable<T> task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        };
    }

    // ... 나머지 ExecutorService 메서드 위임 ...

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrapWithMdc).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrapWithMdc).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks.stream().map(this::wrapWithMdc).toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks.stream().map(this::wrapWithMdc).toList(), timeout, unit);
    }
}
```

### 해결 방법 3: ScopedValue 사용 (JDK 21+ Preview)

**ScopedValue**는 Virtual Thread 환경을 위해 설계된 ThreadLocal의 대안이다.

```java
// ScopedValue 정의 (불변, 상속 가능)
public class RequestContext {
    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
}

// 사용 예시
public void handleRequest(String requestId, String userId) {
    ScopedValue.where(RequestContext.REQUEST_ID, requestId)
        .where(RequestContext.USER_ID, userId)
        .run(() -> {
            // 이 블록 내에서는 어디서든 값 접근 가능
            processRequest();
        });
}

private void processRequest() {
    String requestId = RequestContext.REQUEST_ID.get();
    log.info("Processing request: {}", requestId);

    // Virtual Thread에서도 자동 전파!
    Thread.startVirtualThread(() -> {
        String id = RequestContext.REQUEST_ID.get();  // 정상 동작 ✅
        log.info("Virtual thread has requestId: {}", id);
    }).join();
}

// StructuredTaskScope와 함께 사용
public Response handleRequestWithScope(String requestId) throws Exception {
    return ScopedValue.where(RequestContext.REQUEST_ID, requestId)
        .call(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                // 모든 하위 작업에 requestId 자동 전파
                var userTask = scope.fork(() -> fetchUser());
                var orderTask = scope.fork(() -> fetchOrder());

                scope.join().throwIfFailed();
                return new Response(userTask.get(), orderTask.get());
            }
        });
}
```

### 해결 방법 4: Spring의 TaskDecorator 사용

```java
@Configuration
public class AsyncConfig {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    @Bean
    public Executor virtualThreadExecutor(TaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.setVirtualThreads(true);  // Spring 6.1+
        executor.initialize();
        return executor;
    }
}

// 또는 직접 ThreadFactory 설정
@Bean
public ExecutorService mdcAwareVirtualThreadExecutor() {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual()
            .name("mdc-vthread-", 0)
            .factory()
    );
}
```

### MDC 필터 (Spring Web)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 요청 시작 시 MDC 설정
            String requestId = Optional.ofNullable(httpRequest.getHeader("X-Request-ID"))
                .orElse(UUID.randomUUID().toString());
            String userId = Optional.ofNullable(httpRequest.getHeader("X-User-ID"))
                .orElse("anonymous");

            MDC.put("requestId", requestId);
            MDC.put("userId", userId);
            MDC.put("method", httpRequest.getMethod());
            MDC.put("uri", httpRequest.getRequestURI());
            MDC.put("clientIp", getClientIp(httpRequest));

            chain.doFilter(request, response);

        } finally {
            // 요청 종료 시 MDC 정리
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

### WebFlux에서 MDC (Reactor Context)

```java
// WebFlux는 ThreadLocal 대신 Reactor Context 사용
@Component
public class MdcContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders()
            .getFirst("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        Map<String, String> contextMap = Map.of(
            "requestId", requestId,
            "path", exchange.getRequest().getPath().value()
        );

        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put("MDC", contextMap));
    }
}

// Reactor Context에서 MDC로 복원하는 Hook
@Configuration
public class ReactorMdcConfig {

    @PostConstruct
    public void setupMdcHook() {
        Hooks.onEachOperator("mdc",
            Operators.lift((scannable, subscriber) ->
                new MdcContextSubscriber<>(subscriber)));
    }
}

// 로그 출력 시 Context에서 MDC 복원
public class MdcContextSubscriber<T> extends BaseSubscriber<T> {

    private final CoreSubscriber<? super T> delegate;

    public MdcContextSubscriber(CoreSubscriber<? super T> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void hookOnNext(T value) {
        delegate.currentContext().<Map<String, String>>getOrEmpty("MDC")
            .ifPresent(MDC::setContextMap);
        try {
            delegate.onNext(value);
        } finally {
            MDC.clear();
        }
    }
}
```

### MDC vs ScopedValue 비교

| 구분 | MDC (ThreadLocal) | ScopedValue |
|------|------------------|-------------|
| **설계 목적** | 단일 스레드 | Virtual Thread 환경 |
| **값 변경** | 가변 (put/remove) | 불변 |
| **상속** | 수동 복사 필요 | 자동 상속 |
| **메모리 누수** | clear() 필수 | 스코프 종료 시 자동 정리 |
| **성능** | Virtual Thread에서 비효율 | 최적화됨 |
| **상태** | 정식 | Preview (JDK 21+) |

### MDC 모니터링 Best Practice

```java
@Aspect
@Component
public class MdcMonitoringAspect {

    private final MeterRegistry registry;

    @Around("@annotation(Monitored)")
    public Object monitorWithMdc(ProceedingJoinPoint pjp) throws Throwable {
        String requestId = MDC.get("requestId");

        Timer.Sample sample = Timer.start(registry);

        try {
            return pjp.proceed();
        } catch (Exception e) {
            // 에러 로그에 MDC 정보 포함
            log.error("Error processing request [requestId={}]: {}",
                requestId, e.getMessage(), e);
            registry.counter("errors",
                "requestId", requestId,
                "exception", e.getClass().getSimpleName()
            ).increment();
            throw e;
        } finally {
            sample.stop(registry.timer("request_duration",
                "method", pjp.getSignature().getName()
            ));
        }
    }
}
```

---

## Prometheus + Grafana 연동

### Spring Boot Actuator 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

### Prometheus 설정

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    scrape_interval: 5s
```

### 커스텀 Virtual Thread 메트릭 엔드포인트

```java
@RestController
@RequestMapping("/actuator")
public class VirtualThreadActuator {

    private final MeterRegistry registry;

    @GetMapping("/virtual-threads")
    public Map<String, Object> getVirtualThreadMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // 기본 스레드 정보
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.put("totalThreadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());

        // Virtual Thread 정보
        long virtualThreadCount = Thread.getAllStackTraces().keySet().stream()
            .filter(Thread::isVirtual)
            .count();
        metrics.put("virtualThreadCount", virtualThreadCount);

        // Carrier Thread (ForkJoinPool worker) 정보
        long carrierThreadCount = Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().startsWith("ForkJoinPool") &&
                        t.getName().contains("worker"))
            .count();
        metrics.put("carrierThreadCount", carrierThreadCount);

        // Micrometer 메트릭에서 추가 정보
        Gauge pinnedGauge = registry.find("jvm_virtual_threads_pinned_total").gauge();
        if (pinnedGauge != null) {
            metrics.put("pinnedEventsTotal", pinnedGauge.value());
        }

        return metrics;
    }

    @GetMapping("/virtual-threads/dump")
    public List<Map<String, Object>> getVirtualThreadDump() {
        return Thread.getAllStackTraces().entrySet().stream()
            .filter(e -> e.getKey().isVirtual())
            .map(e -> {
                Map<String, Object> threadInfo = new LinkedHashMap<>();
                Thread t = e.getKey();
                threadInfo.put("id", t.threadId());
                threadInfo.put("name", t.getName());
                threadInfo.put("state", t.getState().toString());
                threadInfo.put("stackTrace", Arrays.stream(e.getValue())
                    .map(StackTraceElement::toString)
                    .limit(20)
                    .toList());
                return threadInfo;
            })
            .toList();
    }
}
```

---

## 실전 모니터링 대시보드

### Grafana 대시보드 JSON

```json
{
  "title": "JVM & Virtual Thread Monitoring",
  "panels": [
    {
      "title": "Virtual Thread Count",
      "type": "stat",
      "targets": [
        {
          "expr": "jvm_virtual_threads_total",
          "legendFormat": "Total"
        }
      ]
    },
    {
      "title": "Virtual Thread Pinning Events",
      "type": "graph",
      "targets": [
        {
          "expr": "rate(jvm_virtual_threads_pinned_total[5m])",
          "legendFormat": "Pinning Rate"
        }
      ],
      "alert": {
        "name": "High Pinning Rate",
        "conditions": [
          {
            "evaluator": { "type": "gt", "params": [10] },
            "operator": { "type": "and" },
            "query": { "params": ["A", "5m", "now"] }
          }
        ]
      }
    },
    {
      "title": "Carrier vs Virtual Threads",
      "type": "graph",
      "targets": [
        {
          "expr": "jvm_carrier_threads",
          "legendFormat": "Carrier Threads"
        },
        {
          "expr": "jvm_virtual_threads_mounted",
          "legendFormat": "Mounted Virtual Threads"
        }
      ]
    },
    {
      "title": "Task Execution Metrics",
      "type": "graph",
      "targets": [
        {
          "expr": "rate(executor_completed_tasks_total[1m])",
          "legendFormat": "Completed/sec"
        },
        {
          "expr": "rate(executor_failed_tasks_total[1m])",
          "legendFormat": "Failed/sec"
        }
      ]
    },
    {
      "title": "JVM Heap Usage",
      "type": "graph",
      "targets": [
        {
          "expr": "jvm_memory_used_bytes{area=\"heap\"}",
          "legendFormat": "Used"
        },
        {
          "expr": "jvm_memory_max_bytes{area=\"heap\"}",
          "legendFormat": "Max"
        }
      ]
    },
    {
      "title": "GC Pause Time",
      "type": "graph",
      "targets": [
        {
          "expr": "rate(jvm_gc_pause_seconds_sum[5m])",
          "legendFormat": "{{gc}}"
        }
      ]
    }
  ]
}
```

### 알림 규칙 예시 (Prometheus Alertmanager)

```yaml
# alerts.yml
groups:
  - name: virtual-thread-alerts
    rules:
      - alert: HighPinningRate
        expr: rate(jvm_virtual_threads_pinned_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High virtual thread pinning rate"
          description: "Pinning rate is {{ $value }}/sec"

      - alert: VirtualThreadStarvation
        expr: executor_active_tasks > 1000 and rate(executor_completed_tasks_total[1m]) < 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Virtual thread starvation detected"
          description: "{{ $value }} active tasks but low completion rate"

      - alert: CarrierThreadExhaustion
        expr: jvm_virtual_threads_mounted / jvm_carrier_threads > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Too many virtual threads per carrier"
          description: "Ratio: {{ $value }}"
```

---

## 요약

| 모니터링 대상 | 도구/방법 | 주요 메트릭 |
|--------------|----------|------------|
| **JVM 기본** | MXBean, Micrometer | Heap, GC, Thread Count |
| **Virtual Thread** | JFR, 커스텀 Gauge | Active, Mounted, Created |
| **Pinning** | JFR, `-Djdk.tracePinnedThreads` | Count, Duration, Stack Trace |
| **ExecutorService** | Micrometer Timer/Counter | Submitted, Completed, Failed |
| **MDC (로깅 컨텍스트)** | ScopedValue, TaskDecorator | requestId, traceId 전파 |

### Pinning 방지 체크리스트

- [ ] `synchronized` → `ReentrantLock` 교체
- [ ] `Object.wait/notify` → `Condition.await/signal` 교체
- [ ] 동기화 블록 내 I/O 작업 제거
- [ ] JFR 기반 Pinning 모니터링 설정
- [ ] CI/CD에 Pinning 감지 테스트 추가

### Virtual Thread에서 MDC 체크리스트

- [ ] Virtual Thread 사용 시 MDC 전파 확인
- [ ] `MDC.getCopyOfContextMap()` + `MDC.setContextMap()` 패턴 적용
- [ ] 또는 ScopedValue 도입 검토 (JDK 21+ Preview)
- [ ] Spring 사용 시 TaskDecorator 설정
- [ ] 요청 종료 시 `MDC.clear()` 호출 확인

*마지막 업데이트: 2026년 01월*
