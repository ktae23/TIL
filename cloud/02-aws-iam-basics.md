# AWS IAM 기초

AWS Identity and Access Management의 기본 개념과 최소 권한 원칙을 정리합니다.

## 목차

1. [IAM 구성 요소](#1-iam-구성-요소)
2. [정책 (Policy)](#2-정책-policy)
3. [역할 (Role)](#3-역할-role)
4. [최소 권한 원칙](#4-최소-권한-원칙)
5. [보안 모범 사례](#5-보안-모범-사례)
6. [실무 예제](#6-실무-예제)

---

## 1. IAM 구성 요소

### 개요

```
┌─────────────────────────────────────────────────────────────┐
│                         IAM                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User (사용자)     - 사람 또는 애플리케이션               │
│       │                                                     │
│       └── Access Key (프로그래밍 방식)                     │
│       └── Password (콘솔 로그인)                           │
│                                                             │
│  Group (그룹)      - 사용자들의 집합                       │
│       │                                                     │
│       └── 그룹에 정책 연결 → 모든 멤버에 적용             │
│                                                             │
│  Role (역할)       - 임시 권한 부여                        │
│       │                                                     │
│       └── EC2, Lambda 등 서비스에서 사용                  │
│       └── 다른 계정에서 사용 (Cross-Account)              │
│                                                             │
│  Policy (정책)     - 권한 정의 문서                        │
│       │                                                     │
│       └── JSON 형식                                        │
│       └── Allow/Deny 명시                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 인증 vs 권한 부여

```
인증 (Authentication): 누구인가?
- 사용자 이름/비밀번호
- 액세스 키
- MFA

권한 부여 (Authorization): 무엇을 할 수 있는가?
- IAM 정책
- 리소스 기반 정책
```

---

## 2. 정책 (Policy)

### 정책 구조

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowS3Read",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::my-bucket",
                "arn:aws:s3:::my-bucket/*"
            ],
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": "203.0.113.0/24"
                }
            }
        }
    ]
}
```

### 주요 요소

```
Version: 정책 언어 버전 (항상 "2012-10-17")
Statement: 권한 명세 배열
  - Sid: 문장 식별자 (선택)
  - Effect: "Allow" 또는 "Deny"
  - Action: 허용/거부할 작업
  - Resource: 대상 리소스 ARN
  - Condition: 조건 (선택)
```

### 정책 유형

```
AWS 관리형 정책:
- AWS가 생성/관리
- 예: AmazonS3ReadOnlyAccess

고객 관리형 정책:
- 사용자가 생성/관리
- 재사용 가능

인라인 정책:
- 단일 사용자/그룹/역할에 직접 연결
- 해당 엔터티와 함께 삭제됨
```

### Action 패턴

```json
// 특정 작업
"Action": "s3:GetObject"

// 와일드카드
"Action": "s3:*"

// 여러 작업
"Action": [
    "s3:GetObject",
    "s3:PutObject"
]

// 접두사 매칭
"Action": "s3:Get*"
```

---

## 3. 역할 (Role)

### 역할 구성

```json
// 신뢰 정책 (Trust Policy): 누가 이 역할을 맡을 수 있는가
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ec2.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}

// 권한 정책 (Permission Policy): 역할이 무엇을 할 수 있는가
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::my-bucket/*"
        }
    ]
}
```

### 역할 사용 시나리오

```
1. EC2 인스턴스에서 S3 접근
   EC2 → IAM Role → S3

2. Lambda에서 DynamoDB 접근
   Lambda → IAM Role → DynamoDB

3. 교차 계정 접근
   Account A User → Assume Role → Account B Resources

4. AWS 서비스 간 연동
   CloudWatch Events → Assume Role → Lambda Invoke
```

### EC2 인스턴스 프로파일

```bash
# EC2에서 역할 사용
# 인스턴스 프로파일에 역할 연결

# EC2에서 자격 증명 자동 획득
aws s3 ls s3://my-bucket/  # 역할 권한으로 실행
```

---

## 4. 최소 권한 원칙

### 원칙

```
"필요한 최소한의 권한만 부여"

Anti-pattern:
{
    "Effect": "Allow",
    "Action": "*",
    "Resource": "*"  // 절대 금지!
}

Best practice:
{
    "Effect": "Allow",
    "Action": [
        "s3:GetObject",
        "s3:PutObject"
    ],
    "Resource": "arn:aws:s3:::my-bucket/uploads/*"
}
```

### 구현 방법

```
1. 필요한 작업 식별
   - 읽기만? 쓰기도?
   - 어떤 작업?

2. 리소스 범위 제한
   - 특정 버킷/테이블만
   - 특정 접두사만

3. 조건 추가
   - IP 제한
   - MFA 필수
   - 시간 제한

4. 정기 검토
   - 사용하지 않는 권한 제거
   - Access Advisor 활용
```

### 조건 활용

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "*",
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": "10.0.0.0/8"
                },
                "Bool": {
                    "aws:MultiFactorAuthPresent": "true"
                },
                "DateLessThan": {
                    "aws:CurrentTime": "2024-12-31T23:59:59Z"
                }
            }
        }
    ]
}
```

---

## 5. 보안 모범 사례

### 루트 계정 보호

```
□ 루트 계정 사용 최소화
□ 루트 계정 MFA 활성화
□ 루트 액세스 키 삭제
□ 강력한 비밀번호 사용
```

### 사용자 관리

```
□ 개별 사용자 계정 생성
□ 그룹으로 권한 관리
□ MFA 필수 적용
□ 비밀번호 정책 강화
□ 정기적 자격 증명 교체
```

### 액세스 키 관리

```
□ 액세스 키 대신 역할 사용 (가능한 경우)
□ 장기 액세스 키 주기적 교체
□ 미사용 액세스 키 비활성화/삭제
□ 코드에 키 하드코딩 금지
```

### 감사 및 모니터링

```
□ CloudTrail 활성화
□ IAM Access Analyzer 사용
□ 정기적 권한 검토
□ 이상 활동 알림 설정
```

---

## 6. 실무 예제

### Lambda 역할 생성

```json
// 신뢰 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "lambda.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}

// 권한 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:GetItem",
                "dynamodb:PutItem"
            ],
            "Resource": "arn:aws:dynamodb:ap-northeast-2:123456789:table/MyTable"
        }
    ]
}
```

### 개발자 그룹 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowDevelopmentResources",
            "Effect": "Allow",
            "Action": [
                "ec2:Describe*",
                "s3:List*",
                "s3:Get*",
                "logs:*"
            ],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Environment": "development"
                }
            }
        },
        {
            "Sid": "DenyProductionAccess",
            "Effect": "Deny",
            "Action": "*",
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Environment": "production"
                }
            }
        }
    ]
}
```

### MFA 필수 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyAllExceptMFASetup",
            "Effect": "Deny",
            "NotAction": [
                "iam:CreateVirtualMFADevice",
                "iam:EnableMFADevice",
                "iam:GetUser",
                "iam:ListMFADevices",
                "iam:ListVirtualMFADevices"
            ],
            "Resource": "*",
            "Condition": {
                "BoolIfExists": {
                    "aws:MultiFactorAuthPresent": "false"
                }
            }
        }
    ]
}
```

---

## 핵심 정리

| 구성 요소 | 용도 |
|----------|------|
| User | 사람 또는 앱 인증 |
| Group | 사용자 그룹화 |
| Role | 임시 권한 (서비스, 교차 계정) |
| Policy | 권한 정의 (JSON) |

| 원칙 | 설명 |
|------|------|
| 최소 권한 | 필요한 최소 권한만 |
| 명시적 거부 | Deny가 Allow보다 우선 |
| 역할 우선 | 장기 키보다 역할 사용 |

---

*마지막 업데이트: 2026년 01월*
