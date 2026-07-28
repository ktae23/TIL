# 기술 문서 영어 표현 가이드

개발자가 실무에서 자주 사용하는 기술 문서 영어 표현을 정리합니다. API 문서, README, 가이드 작성 시 바로 활용할 수 있는 실용적인 예제를 중심으로 구성했습니다.

## 목차

1. [API 문서 작성](#1-api-문서-작성)
2. [README 작성](#2-readme-작성)
3. [에러 메시지 작성](#3-에러-메시지-작성)
4. [코드 주석 작성](#4-코드-주석-작성)
5. [PR/커밋 메시지](#5-pr커밋-메시지)
6. [자주 쓰는 동사와 표현](#6-자주-쓰는-동사와-표현)
7. [실무 필수 어휘](#7-실무-필수-어휘)

---

## 1. API 문서 작성

### 엔드포인트 설명

```markdown
## Create User
Creates a new user account.

**Endpoint:** `POST /api/v1/users`

**Description:**
This endpoint creates a new user with the provided information.
Returns the created user object upon success.
```

**자주 쓰는 표현:**

| 한국어 | 영어 |
|--------|------|
| ~를 생성합니다 | Creates a new ~ |
| ~를 조회합니다 | Retrieves ~ / Fetches ~ |
| ~를 수정합니다 | Updates ~ / Modifies ~ |
| ~를 삭제합니다 | Deletes ~ / Removes ~ |
| ~목록을 반환합니다 | Returns a list of ~ |
| ~가 성공하면 | Upon success / On success |
| ~가 실패하면 | Upon failure / On failure |

### 파라미터 설명

```markdown
### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | The user's email address. Must be unique. |
| `password` | string | Yes | The user's password. Must be at least 8 characters. |
| `name` | string | No | The user's display name. Defaults to email prefix. |
| `role` | string | No | User role. One of: `admin`, `user`, `guest`. Defaults to `user`. |
```

**파라미터 설명 패턴:**

```markdown
# 필수 여부
Required    - 필수
Optional    - 선택
Yes / No    - 표로 표시할 때

# 기본값
Defaults to `value`.
Default: `value`
If not specified, defaults to `value`.

# 제약 조건
Must be unique.
Must be at least 8 characters.
Must be one of: `a`, `b`, `c`.
Cannot be empty.
Cannot exceed 100 characters.

# 형식
Must be a valid email address.
Must be in ISO 8601 format.
Must match the pattern: `^[a-z]+$`
```

### 요청/응답 예시

```markdown
### Request Example

```bash
curl -X POST https://api.example.com/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "name": "John Doe"
  }'
```

### Response

**Success (201 Created)**

```json
{
  "id": "usr_123456",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "user",
  "created_at": "2024-01-15T09:30:00Z"
}
```

**Error (400 Bad Request)**

```json
{
  "error": {
    "code": "INVALID_EMAIL",
    "message": "The provided email address is invalid."
  }
}
```
```

### 인증 설명

```markdown
## Authentication

All API requests require authentication using a Bearer token.

Include the token in the `Authorization` header:

```
Authorization: Bearer your_api_token_here
```

### Obtaining a Token

To obtain an access token, make a POST request to `/auth/token` with your credentials.

### Token Expiration

Access tokens expire after 24 hours. Use the refresh token to obtain a new access token.
```

---

## 2. README 작성

### 프로젝트 소개

```markdown
# Project Name

A brief description of what this project does and who it's for.

## Features

- **Fast**: Optimized for performance
- **Lightweight**: Minimal dependencies
- **Easy to use**: Simple and intuitive API
- **Well documented**: Comprehensive documentation
```

**프로젝트 설명 패턴:**

```markdown
# 간단한 설명
A lightweight library for handling...
A powerful tool for managing...
A simple and fast solution for...

# 대상 사용자
Built for developers who need...
Designed for teams that want to...
Perfect for projects that require...
```

### 설치 가이드

```markdown
## Installation

### Prerequisites

- Node.js 18.0 or higher
- npm 9.0 or higher

### Using npm

```bash
npm install package-name
```

### Using yarn

```bash
yarn add package-name
```

### From source

```bash
git clone https://github.com/user/repo.git
cd repo
npm install
npm run build
```
```

**설치 관련 표현:**

| 한국어 | 영어 |
|--------|------|
| 사전 요구 사항 | Prerequisites |
| ~가 필요합니다 | Requires ~ / ~ is required |
| ~를 설치하세요 | Install ~ |
| 소스에서 빌드 | Build from source |
| 다음 명령어를 실행하세요 | Run the following command |

### 사용 예시

```markdown
## Quick Start

### Basic Usage

```javascript
import { Client } from 'package-name';

// Initialize the client
const client = new Client({
  apiKey: 'your-api-key',
});

// Make a request
const result = await client.getData();
console.log(result);
```

### Advanced Usage

```javascript
// Configure with custom options
const client = new Client({
  apiKey: 'your-api-key',
  timeout: 5000,
  retries: 3,
});

// Handle errors
try {
  const result = await client.getData();
} catch (error) {
  console.error('Failed to fetch data:', error.message);
}
```
```

### 설정 가이드

```markdown
## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `API_KEY` | Your API key | Required |
| `API_URL` | API base URL | `https://api.example.com` |
| `TIMEOUT` | Request timeout in ms | `30000` |
| `DEBUG` | Enable debug logging | `false` |

### Configuration File

Create a `config.json` file in the root directory:

```json
{
  "apiKey": "your-api-key",
  "options": {
    "timeout": 5000,
    "retries": 3
  }
}
```
```

### 기여 가이드

```markdown
## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/repo.git`
3. Install dependencies: `npm install`
4. Create a branch: `git checkout -b feature/your-feature`
5. Make your changes
6. Run tests: `npm test`
7. Commit your changes: `git commit -m 'Add some feature'`
8. Push to the branch: `git push origin feature/your-feature`
9. Open a Pull Request

### Code Style

- Follow the existing code style
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed
```

---

## 3. 에러 메시지 작성

### 사용자 친화적 에러 메시지

```javascript
// Bad
throw new Error('Error');
throw new Error('Invalid');

// Good
throw new Error('Invalid email address. Please enter a valid email.');
throw new Error('Password must be at least 8 characters long.');
throw new Error('User not found. Please check the user ID and try again.');
```

### 에러 메시지 패턴

```javascript
// 입력 검증 에러
'Invalid {field}. {reason}.'
'The {field} is required.'
'The {field} must be {condition}.'

// 예시
'Invalid email. Please enter a valid email address.'
'The password is required.'
'The username must be between 3 and 20 characters.'

// 인증/권한 에러
'Authentication required. Please log in to continue.'
'Access denied. You do not have permission to perform this action.'
'Session expired. Please log in again.'

// 리소스 에러
'{Resource} not found.'
'{Resource} already exists.'
'Unable to create {resource}. {reason}.'

// 예시
'User not found.'
'Email already exists.'
'Unable to create order. Insufficient stock.'

// 서버 에러
'An unexpected error occurred. Please try again later.'
'Service temporarily unavailable. Please try again in a few minutes.'
'Failed to connect to the database. Please contact support.'
```

---

## 4. 코드 주석 작성

### 함수/메서드 주석 (JSDoc 스타일)

```javascript
/**
 * Calculates the total price including tax.
 *
 * @param {number} price - The base price of the item
 * @param {number} quantity - The number of items
 * @param {number} [taxRate=0.1] - The tax rate (default: 10%)
 * @returns {number} The total price including tax
 * @throws {Error} If price or quantity is negative
 *
 * @example
 * const total = calculateTotal(100, 2, 0.08);
 * // Returns: 216
 */
function calculateTotal(price, quantity, taxRate = 0.1) {
  if (price < 0 || quantity < 0) {
    throw new Error('Price and quantity must be non-negative');
  }
  return price * quantity * (1 + taxRate);
}
```

### 인라인 주석

```javascript
// Good - 왜(why) 설명
// Using binary search for O(log n) performance on sorted arrays
const index = binarySearch(sortedArray, target);

// Good - 복잡한 로직 설명
// Calculate the next retry delay using exponential backoff
// Formula: min(maxDelay, baseDelay * 2^attempt)
const delay = Math.min(maxDelay, baseDelay * Math.pow(2, attempt));

// Good - 주의사항
// IMPORTANT: This must be called before any database operations
await initializeConnection();

// TODO 주석
// TODO: Add input validation
// TODO(username): Refactor this to use async/await
// FIXME: This fails when input is empty
// HACK: Temporary fix for issue #123
// NOTE: This assumes the array is already sorted
```

### 클래스 주석

```java
/**
 * Manages user authentication and session handling.
 *
 * <p>This class provides methods for user login, logout, and session
 * validation. It uses JWT tokens for authentication.
 *
 * <p>Usage example:
 * <pre>{@code
 * AuthManager auth = new AuthManager(config);
 * String token = auth.login("user@example.com", "password");
 * boolean isValid = auth.validateToken(token);
 * }</pre>
 *
 * @author John Doe
 * @since 1.0.0
 * @see TokenService
 * @see SessionManager
 */
public class AuthManager {
    // ...
}
```

---

## 5. PR/커밋 메시지

### 커밋 메시지 컨벤션

```bash
# 형식
<type>(<scope>): <subject>

<body>

<footer>

# 타입
feat:     새로운 기능 추가
fix:      버그 수정
docs:     문서 변경
style:    코드 포맷팅 (기능 변경 없음)
refactor: 코드 리팩토링
test:     테스트 추가/수정
chore:    빌드, 설정 변경

# 예시
feat(auth): add password reset functionality

Add forgot password and reset password endpoints.
Users can now request a password reset email and
set a new password using the reset token.

Closes #123
```

### PR 설명 작성

```markdown
## Summary

Add user authentication using JWT tokens.

## Changes

- Add login and logout endpoints
- Implement JWT token generation and validation
- Add authentication middleware
- Update user model with password hashing

## Testing

- [x] Unit tests for auth service
- [x] Integration tests for auth endpoints
- [x] Manual testing with Postman

## Screenshots (if applicable)

N/A

## Checklist

- [x] Code follows the project's style guide
- [x] Tests pass locally
- [x] Documentation updated
- [ ] Ready for review
```

### PR 리뷰 표현

```markdown
# 승인
LGTM (Looks Good To Me)
Approved! Nice work.
Ship it! 🚀

# 질문
Could you explain why we need this?
What happens if the input is null?
Have you considered using X instead?

# 제안
Consider using a more descriptive variable name here.
This could be simplified to: `const x = a || b`
Nit: Add a blank line here for readability.

# 요청
Please add a test case for the edge case.
This needs error handling.
Could you add a comment explaining this logic?
```

---

## 6. 자주 쓰는 동사와 표현

### 동작 동사

| 동사 | 용도 | 예시 |
|------|------|------|
| Create | 새로 생성 | Creates a new user |
| Add | 기존에 추가 | Adds a new item to the list |
| Remove | 제거 | Removes the item from the cart |
| Delete | 완전 삭제 | Deletes the user permanently |
| Update | 수정 | Updates the user profile |
| Set | 값 설정 | Sets the default value |
| Get | 값 조회 | Gets the current user |
| Fetch | 외부에서 가져옴 | Fetches data from the API |
| Retrieve | 저장소에서 조회 | Retrieves the record from database |
| Return | 반환 | Returns the calculated result |
| Handle | 처리 | Handles the form submission |
| Process | 가공/처리 | Processes the payment |
| Validate | 검증 | Validates the input data |
| Check | 확인 | Checks if the user exists |
| Verify | 검증 (인증) | Verifies the user's identity |
| Initialize | 초기화 | Initializes the database connection |
| Configure | 설정 | Configures the application settings |
| Enable | 활성화 | Enables dark mode |
| Disable | 비활성화 | Disables notifications |

### 상태/결과 표현

```markdown
# 성공
Successfully created the user.
The operation completed successfully.
Done.

# 실패
Failed to create the user.
Unable to connect to the server.
The operation could not be completed.

# 진행 중
Loading...
Processing your request...
Please wait...

# 경고
This action cannot be undone.
Are you sure you want to continue?
Warning: This will delete all data.
```

### 조건/상황 표현

```markdown
# 조건
If the user is not found, returns null.
When the request fails, retries up to 3 times.
Unless specified otherwise, uses the default value.

# 주의사항
Note: This feature is experimental.
Important: Back up your data before proceeding.
Warning: This action is irreversible.
Caution: Do not share your API key.

# 제한/요구사항
Requires Node.js 18 or higher.
Only available for premium users.
Limited to 100 requests per minute.
```

---

## 실전 템플릿

### API 엔드포인트 전체 예시

```markdown
## List Users

Retrieves a paginated list of users.

**Endpoint:** `GET /api/v1/users`

**Authentication:** Required

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | integer | No | Page number. Defaults to `1`. |
| `limit` | integer | No | Items per page. Max `100`. Defaults to `20`. |
| `sort` | string | No | Sort field. One of: `created_at`, `name`. Defaults to `created_at`. |
| `order` | string | No | Sort order. One of: `asc`, `desc`. Defaults to `desc`. |

### Response

**Success (200 OK)**

```json
{
  "data": [
    {
      "id": "usr_123",
      "email": "user@example.com",
      "name": "John Doe",
      "created_at": "2024-01-15T09:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "total_pages": 8
  }
}
```

### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 401 | `UNAUTHORIZED` | Authentication required |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 429 | `RATE_LIMITED` | Too many requests |
```

---

## 7. 실무 필수 어휘

### 계정/사용자 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Account | 계정 | Your account has been suspended. |
| Credential | 자격 증명, 인증 정보 | Enter your credentials to log in. |
| Permission | 권한 | You don't have permission to access this resource. |
| Role | 역할 | Assign a role to the user. |
| Session | 세션 | Your session has expired. |
| Token | 토큰 | The access token is invalid. |
| Profile | 프로필 | Update your profile information. |
| Subscription | 구독 | Your subscription will renew automatically. |
| Tier | 등급, 티어 | Upgrade to a higher tier for more features. |
| Quota | 할당량 | You have exceeded your API quota. |

**예문:**
```
- The account is not eligible for this promotion.
- Please verify your credentials and try again.
- This action requires admin permissions.
```

### 자격/조건 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Eligible | 자격이 있는 | Only eligible users can access this feature. |
| Applicable | 적용 가능한 | This discount is applicable to annual plans only. |
| Valid | 유효한 | Please enter a valid email address. |
| Invalid | 유효하지 않은 | The provided token is invalid. |
| Expired | 만료된 | Your trial period has expired. |
| Active | 활성화된 | Only active subscriptions can use this feature. |
| Inactive | 비활성화된 | The user account is inactive. |
| Pending | 대기 중인 | Your request is pending approval. |
| Approved | 승인된 | Your application has been approved. |
| Rejected | 거절된 | The request was rejected due to invalid data. |

**예문:**
```
- Users are eligible for a free trial once per account.
- This offer is only applicable to new customers.
- The coupon code is no longer valid.
```

### 데이터/상태 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Payload | 페이로드, 전송 데이터 | The request payload is too large. |
| Entity | 엔티티, 개체 | The entity could not be found. |
| Record | 레코드, 기록 | No matching records found. |
| Entry | 항목 | Add a new entry to the database. |
| Instance | 인스턴스 | Create a new instance of the service. |
| Resource | 리소스, 자원 | The requested resource does not exist. |
| Attribute | 속성 | The attribute value is required. |
| Property | 속성, 프로퍼티 | Set the property to enable this feature. |
| Metadata | 메타데이터 | Include metadata in the response. |
| Schema | 스키마 | The data does not match the expected schema. |

**예문:**
```
- The payload must be in JSON format.
- Each entity has a unique identifier.
- This resource has been deprecated.
```

### 처리/동작 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Invoke | 호출하다 | Invoke the function with the required parameters. |
| Trigger | 트리거, 발동시키다 | The webhook is triggered on every update. |
| Execute | 실행하다 | Execute the query against the database. |
| Propagate | 전파하다 | Changes propagate to all connected clients. |
| Persist | 영속화하다 | Persist the data to the database. |
| Cache | 캐시하다 | Cache the response for better performance. |
| Throttle | 제한하다 | Requests are throttled to 100 per minute. |
| Retry | 재시도하다 | Retry the request after 5 seconds. |
| Queue | 대기열에 넣다 | The job has been queued for processing. |
| Dispatch | 디스패치, 전달하다 | Dispatch the event to all listeners. |

**예문:**
```
- The function is invoked automatically on startup.
- This event triggers a notification to the user.
- Failed requests are automatically retried up to 3 times.
```

### 설정/구성 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Configuration | 설정, 구성 | Check your configuration settings. |
| Parameter | 파라미터, 매개변수 | Pass the required parameters to the function. |
| Option | 옵션, 선택 사항 | This option is enabled by default. |
| Flag | 플래그 | Set the debug flag to true for verbose logging. |
| Variable | 변수 | Set the environment variable before running. |
| Constant | 상수 | Define constants at the top of the file. |
| Preference | 설정, 기본 설정 | Update your notification preferences. |
| Setting | 설정 | Adjust the settings as needed. |
| Specification | 명세, 사양 | Follow the API specification. |
| Constraint | 제약 조건 | Add a unique constraint to the column. |

**예문:**
```
- The configuration file must be in YAML format.
- All parameters are optional unless specified otherwise.
- Enable this flag to activate the feature.
```

### 오류/예외 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Exception | 예외 | An unexpected exception occurred. |
| Failure | 실패 | The operation ended in failure. |
| Timeout | 타임아웃, 시간 초과 | The request timed out after 30 seconds. |
| Conflict | 충돌 | A conflict occurred with the existing data. |
| Violation | 위반 | Constraint violation: duplicate key. |
| Overflow | 오버플로우 | Stack overflow detected. |
| Leak | 누수 | Memory leak detected in the application. |
| Deadlock | 교착 상태 | A deadlock was detected and resolved. |
| Bottleneck | 병목 | Database queries are the main bottleneck. |
| Latency | 지연 시간 | High latency detected on the network. |

**예문:**
```
- Handle the exception gracefully.
- The request failed due to a timeout.
- Resolve the conflict before merging.
```

### 보안 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Authentication | 인증 | Authentication is required for this endpoint. |
| Authorization | 인가, 권한 부여 | Authorization failed: insufficient permissions. |
| Encryption | 암호화 | All data is encrypted at rest. |
| Decryption | 복호화 | Decryption failed: invalid key. |
| Hash | 해시 | Store the password as a hash. |
| Salt | 솔트 | Add a salt before hashing the password. |
| Vulnerability | 취약점 | A critical vulnerability was discovered. |
| Exploit | 익스플로잇, 취약점 공격 | Patch the vulnerability before it can be exploited. |
| Breach | 침해, 유출 | Report any suspected data breach immediately. |
| Compliance | 규정 준수 | Ensure compliance with GDPR requirements. |

**예문:**
```
- Two-factor authentication is recommended.
- The API uses OAuth 2.0 for authorization.
- All sensitive data is encrypted in transit.
```

### 성능/확장 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Scalable | 확장 가능한 | The architecture is designed to be scalable. |
| Throughput | 처리량 | Increase throughput by adding more workers. |
| Concurrency | 동시성 | Handle concurrency with proper locking. |
| Parallel | 병렬 | Process tasks in parallel for better performance. |
| Asynchronous | 비동기 | Use asynchronous calls to avoid blocking. |
| Synchronous | 동기 | Synchronous operations block the main thread. |
| Optimize | 최적화하다 | Optimize the query for better performance. |
| Benchmark | 벤치마크 | Run benchmarks to measure performance. |
| Profiling | 프로파일링 | Use profiling to identify bottlenecks. |
| Load balancing | 로드 밸런싱 | Enable load balancing for high availability. |

**예문:**
```
- The system is horizontally scalable.
- Throughput increased by 50% after optimization.
- Use asynchronous processing for long-running tasks.
```

### 배포/운영 관련

| 영어 | 한국어 | 예문 |
|------|--------|------|
| Deploy | 배포하다 | Deploy the application to production. |
| Release | 릴리스, 배포 | The new release includes bug fixes. |
| Rollback | 롤백 | Rollback to the previous version if issues occur. |
| Provision | 프로비저닝 | Provision new servers automatically. |
| Monitor | 모니터링하다 | Monitor the system for anomalies. |
| Alert | 알림, 경고 | Set up alerts for critical errors. |
| Downtime | 다운타임 | Schedule maintenance during low-traffic hours to minimize downtime. |
| Uptime | 가동 시간 | We guarantee 99.9% uptime. |
| Incident | 인시던트, 장애 | Report the incident to the on-call engineer. |
| Outage | 서비스 중단 | The outage lasted approximately 2 hours. |

**예문:**
```
- Deploy the changes to the staging environment first.
- Automatic rollback is triggered on deployment failure.
- Monitor CPU and memory usage in real-time.
```

### 자주 혼동되는 표현

| 표현 | 의미 | 올바른 사용 |
|------|------|------------|
| i.e. | 즉 (that is) | Use JSON format, i.e., `{"key": "value"}`. |
| e.g. | 예를 들어 (for example) | Supports multiple formats, e.g., JSON, XML, YAML. |
| etc. | 기타 등등 | Includes headers, body, params, etc. |
| N/A | 해당 없음 | If not applicable, enter N/A. |
| TBD | 미정 (To Be Determined) | The release date is TBD. |
| TL;DR | 요약 (Too Long; Didn't Read) | TL;DR: Use async for better performance. |
| ASAP | 가능한 빨리 | Please fix this issue ASAP. |
| FYI | 참고로 | FYI, the API rate limit has been updated. |
| LGTM | 좋아 보임 (코드 리뷰) | LGTM! Ready to merge. |
| WIP | 작업 중 (Work In Progress) | WIP: Add user authentication |

---

*마지막 업데이트: 2026년 01월*
