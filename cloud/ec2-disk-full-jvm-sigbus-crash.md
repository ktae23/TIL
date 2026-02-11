# EC2 디스크 풀로 인한 JVM SIGBUS 크래시 및 SSH 장애

## 개요

EC2 인스턴스의 디스크 사용률이 90%를 초과한 뒤 배치 프로세스(JVM)가 비정상 종료되고, 이후 SSH 접속까지 불가능해진 장애를 분석한다. EBS 볼륨을 2배로 확장한 뒤에야 접속이 복구되었다.

## 목차

- [장애 타임라인](#장애-타임라인)
- [SIGBUS(0x7)란 무엇인가](#sigbus0x7란-무엇인가)
- [왜 디스크 풀이 JVM을 죽이는가](#왜-디스크-풀이-jvm을-죽이는가)
- [왜 SSH까지 불가능해지는가](#왜-ssh까지-불가능해지는가)
- [복구 과정](#복구-과정)
- [재발 방지 대책](#재발-방지-대책)
- [참고 자료](#참고-자료)

---

## 장애 타임라인

```
디스크 사용률 90%+ 알림 발생
        ↓
배치 프로세스(JVM)가 /tmp 또는 디스크에 파일 쓰기 시도
        ↓
공간 부족 → mmap된 파일 접근 시 SIGBUS (0x7) 발생
        ↓
JVM 크래시 (비정상 종료, graceful shutdown 불가)
        ↓
디스크 100% 도달 → OS 레벨 장애 확산
        ↓
SSH 접속 불가 (sshd가 새 세션 생성 실패)
        ↓
AWS 콘솔에서 EBS 볼륨 2배 확장 → 공간 확보 → 접속 복구
```

## SIGBUS(0x7)란 무엇인가

**SIGBUS(Bus Error)** 는 프로세스가 유효하지 않은 메모리 영역에 접근할 때 커널이 보내는 시그널이다. SIGSEGV(Segmentation Fault)와 혼동하기 쉽지만, 둘은 다르다.

| 시그널 | 번호 | 원인 |
|--------|------|------|
| **SIGSEGV** | 11 | 매핑되지 않은 메모리 주소에 접근 |
| **SIGBUS** | 7 (0x7) | 매핑은 되어 있지만 물리적으로 접근 불가능한 메모리 |

디스크 풀 상황에서 SIGBUS가 발생하는 이유는 `mmap()` 시스템 콜과 관련이 있다. 파일을 메모리에 매핑했는데, 그 파일이 위치한 디스크에 더 이상 블록을 할당할 수 없으면 커널은 해당 페이지에 대한 접근을 SIGBUS로 거부한다.

## 왜 디스크 풀이 JVM을 죽이는가

### JVM의 mmap 사용

JVM은 내부적으로 여러 곳에서 `mmap()`을 사용한다:

1. **JAR 파일 읽기**: `libzip.so`를 통해 JAR 파일을 mmap으로 매핑하여 클래스를 로딩
2. **임시 파일**: JIT 컴파일 결과, 임시 데이터를 `/tmp`에 기록
3. **Heap 외 메모리**: Direct ByteBuffer, MappedByteBuffer 등

### 크래시 메커니즘

```
1. JVM이 mmap()으로 파일을 메모리에 매핑
2. 매핑된 영역에 쓰기 시도 (예: 새 클래스 로딩, 임시 파일 생성)
3. OS가 물리적 디스크 블록 할당을 시도하지만 공간 없음
4. 커널이 SIGBUS (0x7) 시그널 전송
5. JVM은 이 시그널을 핸들링하지 못하고 즉시 종료
```

핵심은 **JVM이 SIGBUS를 잡을 수 없다**는 점이다. OutOfMemoryError처럼 예외를 던지며 graceful하게 종료하는 것이 아니라, OS 시그널에 의해 프로세스가 즉사한다. 심지어 크래시 덤프(`hs_err_pid*.log`)조차 디스크 부족으로 기록되지 못하는 경우가 많다.

> **Oracle Java Bug ID: 7007769**
> Java 6u21에서 수정되었으나, 디스크 풀 상황에서는 최신 JVM에서도 동일한 문제가 발생할 수 있다.

## 왜 SSH까지 불가능해지는가

SSH 데몬(sshd)이 새 세션을 생성하려면 여러 파일에 쓰기가 필요하다:

| 경로 | 용도 |
|------|------|
| `/var/log/auth.log` (또는 `/var/log/secure`) | 인증 로그 기록 |
| `/tmp` | 세션 임시 파일 |
| `/var/run` | PID 파일, 소켓 파일 |
| `~/.ssh/` | known_hosts 업데이트 등 |

디스크가 100%이면 이 파일들을 생성/기록할 수 없어 **sshd가 새 세션을 열 수 없다.** 기존 연결된 세션도 명령어 실행 시 로그 기록 실패로 오류가 발생할 수 있다.

### ext4 예약 블록(Reserved Blocks)과의 관계

ext4 파일시스템은 기본적으로 전체 용량의 **5%를 root 사용자 전용으로 예약**한다. 이 예약 블록이 있으면 일반 사용자가 디스크를 100% 채워도 root는 여전히 파일을 쓸 수 있어 SSH 접속이 유지된다.

만약 이 예약 비율이 0%로 설정되어 있었다면, root조차 쓸 공간이 없어져 SSH가 완전히 막힌다.

```bash
# 현재 예약 블록 비율 확인
sudo tune2fs -l /dev/xvda1 | grep "Reserved block count"

# 5%로 설정 (권장)
sudo tune2fs -m 5 /dev/xvda1
```

## 복구 과정

이 장애에서는 AWS 콘솔에서 EBS 볼륨 크기를 2배로 확장하여 복구하였다.

```bash
# 1. AWS 콘솔 또는 CLI로 EBS 볼륨 크기 확장
aws ec2 modify-volume --volume-id vol-xxxx --size 100  # 예: 50GB → 100GB

# 2. EC2 인스턴스에 접속 후 파티션 확장
sudo growpart /dev/xvda 1

# 3. 파일시스템 확장
sudo resize2fs /dev/xvda1        # ext4인 경우
# sudo xfs_growfs /dev/xvda1     # xfs인 경우
```

> EBS 볼륨 확장은 인스턴스 재부팅 없이 온라인 상태에서 가능하다. 다만 볼륨 크기 확장 후 OS에서 파티션과 파일시스템을 확장하는 작업이 추가로 필요하다.

## 재발 방지 대책

### 1. 모니터링 임계값 조정

디스크 사용률 90%에서 알림을 받으면 이미 늦다. 단계별 알림을 설정한다.

| 임계값 | 액션 |
|--------|------|
| **70%** | Warning 알림 → 로그 정리 검토 |
| **80%** | Critical 알림 → 즉시 대응 |
| **90%** | Emergency → 자동 볼륨 확장 트리거 |

### 2. 로그 로테이션 설정

배치 프로세스의 로그가 디스크를 채우는 주범인 경우가 많다.

```xml
<!-- logback-spring.xml -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/batch-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>7</maxHistory>
        <totalSizeCap>1GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### 3. /tmp 격리

`/tmp`를 별도 파티션이나 tmpfs로 분리하면 루트 디스크와 격리되어 JVM 크래시를 방지할 수 있다.

```bash
# tmpfs로 /tmp 마운트 (메모리 기반, 재부팅 시 초기화)
echo "tmpfs /tmp tmpfs defaults,size=2G 0 0" >> /etc/fstab
```

또는 JVM 레벨에서 임시 디렉토리를 여유 있는 볼륨으로 변경:

```bash
java -Djava.io.tmpdir=/data/tmp -jar batch-app.jar
```

### 4. ext4 예약 블록 확보

```bash
# root 전용 예약 블록을 5%로 설정
sudo tune2fs -m 5 /dev/xvda1
```

이렇게 하면 디스크가 95%까지 차도 root로 SSH 접속이 가능하다.

### 5. 디스크 자동 확장

CloudWatch 알람과 Lambda를 연동하여 자동 확장 파이프라인을 구성할 수 있다.

```
CloudWatch Alarm (디스크 90%)
        ↓
SNS Topic
        ↓
Lambda 함수 실행
        ↓
EBS modify-volume + growpart + resize2fs
```

### 6. 불필요한 파일 정기 삭제

cron으로 오래된 로그, 임시 파일을 정기적으로 정리한다.

```bash
# 7일 이상 된 /tmp 파일 삭제 (crontab)
0 3 * * * find /tmp -type f -mtime +7 -delete 2>/dev/null

# 배치 로그 30일 초과 삭제
0 3 * * * find /var/log/batch -type f -mtime +30 -delete 2>/dev/null
```

## 핵심 정리

1. **디스크 풀 → JVM SIGBUS 크래시**: JVM이 mmap으로 매핑한 영역에 쓰기 시도 시 디스크 블록 할당 불가로 SIGBUS 발생, 즉시 사망
2. **디스크 풀 → SSH 불가**: sshd도 로그/임시파일 쓰기가 필요하므로 디스크 0%에서는 새 세션 생성 불가
3. **예방의 핵심**: 예약 블록(tune2fs -m), 단계별 알림, 로그 로테이션, /tmp 격리

## 참고 자료

- [Atlassian - Java VM dies with SIGBUS (0x7) when temp directory is full on Linux](https://support.atlassian.com/confluence/kb/java-vm-dies-with-sigbus-0x7-when-temp-directory-is-full-on-linux/)
- [Oracle Java Bug ID: 7007769](https://bugs.java.com/bugdatabase/view_bug?bug_id=7007769)
- [AWS EBS 볼륨 크기 수정](https://docs.aws.amazon.com/ebs/latest/userguide/requesting-ebs-volume-modifications.html)

*마지막 업데이트: 2025년 02월*
