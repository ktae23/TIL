# AWS 장애 대응 가이드

CloudWatch 알람 설정과 장애 대응 절차를 정리합니다.

## 목차

1. [CloudWatch 모니터링](#1-cloudwatch-모니터링)
2. [알람 설정](#2-알람-설정)
3. [장애 감지 패턴](#3-장애-감지-패턴)
4. [대응 절차](#4-대응-절차)
5. [자동화된 대응](#5-자동화된-대응)
6. [포스트모템](#6-포스트모템)

---

## 1. CloudWatch 모니터링

### 핵심 메트릭

```
EC2:
- CPUUtilization
- StatusCheckFailed
- NetworkIn/Out
- DiskReadOps/WriteOps

RDS:
- CPUUtilization
- FreeableMemory
- ReadIOPS/WriteIOPS
- DatabaseConnections
- ReplicaLag

ALB:
- RequestCount
- TargetResponseTime
- HTTPCode_Target_5XX_Count
- HealthyHostCount

Lambda:
- Invocations
- Errors
- Duration
- Throttles
- ConcurrentExecutions
```

### 커스텀 메트릭

```python
# Python으로 커스텀 메트릭 발행
import boto3

cloudwatch = boto3.client('cloudwatch')

cloudwatch.put_metric_data(
    Namespace='MyApp',
    MetricData=[
        {
            'MetricName': 'OrdersProcessed',
            'Value': 42,
            'Unit': 'Count',
            'Dimensions': [
                {'Name': 'Environment', 'Value': 'Production'}
            ]
        }
    ]
)
```

### 로그 기반 메트릭

```
CloudWatch Logs → Metric Filter → 알람

필터 패턴 예시:
- ERROR: [ERROR]
- Exception: Exception
- 5xx 오류: "HTTP/1.1\" 5"
- 느린 쿼리: "slow query"
```

---

## 2. 알람 설정

### 알람 구성 요소

```yaml
알람 구성:
- MetricName: CPUUtilization
- Statistic: Average
- Period: 300 (5분)
- EvaluationPeriods: 2
- Threshold: 80
- ComparisonOperator: GreaterThanThreshold
- AlarmActions: [SNS Topic ARN]
```

### CloudFormation 예시

```yaml
Resources:
  HighCPUAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: !Sub "${Environment}-high-cpu"
      AlarmDescription: "CPU utilization > 80%"
      MetricName: CPUUtilization
      Namespace: AWS/EC2
      Statistic: Average
      Period: 300
      EvaluationPeriods: 2
      Threshold: 80
      ComparisonOperator: GreaterThanThreshold
      Dimensions:
        - Name: InstanceId
          Value: !Ref MyEC2Instance
      AlarmActions:
        - !Ref AlertSNSTopic
      OKActions:
        - !Ref AlertSNSTopic
```

### 복합 알람

```yaml
# 여러 조건을 AND/OR로 결합
CompositeAlarm:
  Type: AWS::CloudWatch::CompositeAlarm
  Properties:
    AlarmName: ServiceDegraded
    AlarmRule: >
      ALARM(HighCPUAlarm) AND
      (ALARM(HighErrorRateAlarm) OR ALARM(HighLatencyAlarm))
    AlarmActions:
      - !Ref PagerDutyTopic
```

### 권장 알람 목록

```
우선순위 1 (즉시 대응):
- 서비스 다운 (HealthyHostCount = 0)
- 5xx 에러 급증
- 데이터베이스 연결 불가

우선순위 2 (30분 내 확인):
- CPU > 80% (지속)
- 메모리 < 10%
- 디스크 > 85%
- 응답 시간 > 3초

우선순위 3 (업무 시간 확인):
- Replica Lag > 10초
- 비정상적 트래픽 증가
- 예상치 못한 비용 증가
```

---

## 3. 장애 감지 패턴

### 단일 지표 알람

```
CPU 과부하:
- 메트릭: CPUUtilization > 80%
- 기간: 5분
- 평가 기간: 2회 연속
```

### 이상 탐지

```yaml
# CloudWatch Anomaly Detection
AnomalyDetectionAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: AnomalousRequestCount
    MetricName: RequestCount
    Namespace: AWS/ApplicationELB
    Statistic: Sum
    Period: 300
    EvaluationPeriods: 2
    ThresholdMetricId: ad1
    ComparisonOperator: LessThanLowerOrGreaterThanUpperThreshold
    Metrics:
      - Id: m1
        MetricStat:
          Metric:
            MetricName: RequestCount
            Namespace: AWS/ApplicationELB
          Period: 300
          Stat: Sum
      - Id: ad1
        Expression: ANOMALY_DETECTION_BAND(m1, 2)
```

### 비율 기반 알람

```yaml
# 에러율 계산
ErrorRateAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: HighErrorRate
    EvaluationPeriods: 2
    Threshold: 5  # 5% 이상
    ComparisonOperator: GreaterThanThreshold
    Metrics:
      - Id: errors
        MetricStat:
          Metric:
            MetricName: HTTPCode_Target_5XX_Count
            Namespace: AWS/ApplicationELB
          Period: 300
          Stat: Sum
      - Id: requests
        MetricStat:
          Metric:
            MetricName: RequestCount
            Namespace: AWS/ApplicationELB
          Period: 300
          Stat: Sum
      - Id: errorRate
        Expression: (errors / requests) * 100
        Label: ErrorRate
        ReturnData: true
```

---

## 4. 대응 절차

### 장애 대응 단계

```
1. 탐지 (Detection)
   - 알람 수신
   - 영향 범위 파악

2. 진단 (Diagnosis)
   - 로그 확인
   - 메트릭 분석
   - 최근 변경 사항 확인

3. 완화 (Mitigation)
   - 즉각적 조치
   - 롤백 또는 스케일 아웃

4. 해결 (Resolution)
   - 근본 원인 수정
   - 테스트 및 배포

5. 사후 분석 (Post-incident)
   - 포스트모템 작성
   - 재발 방지 조치
```

### 빠른 진단 명령어

```bash
# EC2 상태 확인
aws ec2 describe-instance-status --instance-id i-xxx

# RDS 이벤트 확인
aws rds describe-events --source-type db-instance \
  --source-identifier mydb --duration 60

# ECS 서비스 상태
aws ecs describe-services --cluster mycluster \
  --services myservice

# Lambda 로그 확인
aws logs filter-log-events --log-group-name /aws/lambda/myfunction \
  --start-time $(date -d '10 minutes ago' +%s000) \
  --filter-pattern "ERROR"
```

### 롤백 절차

```bash
# EC2 Auto Scaling 롤백
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name my-asg \
  --launch-template LaunchTemplateId=lt-xxx,Version=1

# ECS 롤백
aws ecs update-service --cluster mycluster \
  --service myservice \
  --task-definition mytask:123  # 이전 버전

# Lambda 롤백
aws lambda update-alias --function-name myfunction \
  --name prod --function-version 42  # 이전 버전
```

---

## 5. 자동화된 대응

### EventBridge + Lambda

```python
# 장애 시 자동 대응 Lambda
import boto3

def handler(event, context):
    ec2 = boto3.client('ec2')
    sns = boto3.client('sns')

    alarm_name = event['detail']['alarmName']
    instance_id = extract_instance_id(alarm_name)

    # 자동 복구 시도
    ec2.reboot_instances(InstanceIds=[instance_id])

    # 알림 발송
    sns.publish(
        TopicArn='arn:aws:sns:...',
        Subject=f'Auto-recovery initiated: {instance_id}',
        Message=f'Instance {instance_id} has been rebooted due to alarm {alarm_name}'
    )
```

### Systems Manager Automation

```yaml
# SSM Automation 문서
schemaVersion: '0.3'
description: 'Auto-remediate high CPU'
assumeRole: '{{AutomationAssumeRole}}'
parameters:
  InstanceId:
    type: String
mainSteps:
  - name: RestartInstance
    action: aws:executeAwsApi
    inputs:
      Service: ec2
      Api: RebootInstances
      InstanceIds:
        - '{{InstanceId}}'
  - name: WaitForRestart
    action: aws:sleep
    inputs:
      Duration: PT2M
  - name: VerifyRecovery
    action: aws:executeAwsApi
    inputs:
      Service: ec2
      Api: DescribeInstanceStatus
      InstanceIds:
        - '{{InstanceId}}'
```

### Auto Scaling 정책

```yaml
# 장애 시 자동 스케일 아웃
TargetTrackingScalingPolicy:
  Type: AWS::AutoScaling::ScalingPolicy
  Properties:
    AutoScalingGroupName: !Ref ASG
    PolicyType: TargetTrackingScaling
    TargetTrackingConfiguration:
      PredefinedMetricSpecification:
        PredefinedMetricType: ASGAverageCPUUtilization
      TargetValue: 70.0
      ScaleOutCooldown: 60
      ScaleInCooldown: 300
```

---

## 6. 포스트모템

### 포스트모템 템플릿

```markdown
# 장애 보고서: [제목]

## 요약
- 발생 일시: YYYY-MM-DD HH:MM ~ HH:MM
- 영향 범위: [서비스, 사용자 수]
- 심각도: Critical / High / Medium / Low

## 타임라인
- HH:MM - 최초 알람 발생
- HH:MM - 담당자 확인
- HH:MM - 원인 파악
- HH:MM - 조치 시작
- HH:MM - 서비스 복구
- HH:MM - 정상 확인

## 근본 원인
[원인 상세 설명]

## 영향
- 사용자 영향: [설명]
- 비즈니스 영향: [설명]

## 조치 사항
1. [조치 1]
2. [조치 2]

## 재발 방지
1. [개선 사항 1]
2. [개선 사항 2]

## 교훈
- [배운 점]
```

### 재발 방지 액션 아이템

```
□ 모니터링 개선
  - 추가 알람 설정
  - 대시보드 업데이트

□ 자동화 강화
  - 자동 복구 스크립트
  - 롤백 자동화

□ 문서화
  - 런북 업데이트
  - 장애 대응 절차 갱신

□ 아키텍처 개선
  - SPOF 제거
  - 이중화 강화
```

---

## 핵심 체크리스트

```
장애 대응 준비:
□ 핵심 메트릭 알람 설정
□ 알림 채널 구성 (Slack, PagerDuty)
□ 런북 작성
□ 롤백 절차 테스트

장애 발생 시:
□ 영향 범위 파악
□ 이해관계자 알림
□ 로그/메트릭 확인
□ 롤백 또는 스케일 조치

장애 후:
□ 포스트모템 작성
□ 재발 방지 조치
□ 문서 업데이트
```

---

*마지막 업데이트: 2026년 01월*
