# TIL — Today I Learned

> **📖 [웹 뷰어로 보기 → ktae23.github.io/TIL](https://ktae23.github.io/TIL)**
>
> 검색(`Ctrl+K`), 다크/라이트 테마, 목차 자동 생성을 지원합니다. 마크다운을 직접 읽는 것보다 뷰어가 편합니다.

백엔드 학습 기록 저장소입니다. 공식 문서와 **오픈소스 원본 소스를 직접 읽고** 정리하는 것을 원칙으로 하며,
각 문서는 `핵심 개념(What) → 왜 알아야 하는가(Why) → 내부 구현 분석(How) → 실전 예제 → 정리` 구조를 따릅니다.

현재 **530개 문서**, 7개 그룹.

## 카테고리

| 그룹 | 주요 주제 |
|---|---|
| 🖥️ **Backend Framework** (143) | [Reactive (Reactor & WebClient)](https://ktae23.github.io/TIL/#reactive%2Fmain%2F01-reactive-streams-flux-mono.md) · [Spring Batch](https://ktae23.github.io/TIL/#batch%2Fmain%2F01-spring-batch-fundamentals.md) · Hibernate · JPA · Spring Data JDBC · Spring |
| 💻 **Language & Runtime** (44) | Kotlin · Java (Virtual Thread) · Python |
| 🗄️ **Data & Messaging** (78) | Redis · MySQL · Kafka · Cache |
| ☁️ **Infrastructure** (120) | Cloud (Docker·K8s·SRE) · ELK Stack · AWS CI/CD & ECS · Observability · DevOps |
| 🏗️ **Architecture & Design** (79) | [Resilience4j](https://ktae23.github.io/TIL/#resilience4j%2Fmain%2F01-architecture-overview.md) · DDD · MSA · Network |
| ✅ **Quality & Testing** (30) | Testing (부하 테스트) · Security |
| 📚 **General** (36) | Algorithm · 기술 영어 · CS · SEO |

## 깊이 있게 다룬 커리큘럼

소스 코드를 `파일:줄` 단위로 인용해 내부 구현까지 추적한 시리즈입니다.

- **[Reactor & WebClient](https://ktae23.github.io/TIL/#reactive%2Fmain%2F01-reactive-streams-flux-mono.md)** (24문서) — 조립/구독 시점 분리, 오퍼레이터 퓨전, drain 루프, 커넥션 풀, 타임아웃 7계층
  <br>*Reactor Core 3.7.19 · Spring Framework 6.2.19 · reactor-netty 1.2.18 기준 (+ Spring 7.0.9 마이그레이션)*
- **[Resilience4j](https://ktae23.github.io/TIL/#resilience4j%2Fmain%2F01-architecture-overview.md)** (24문서) — CircuitBreaker 상태 기계, 슬라이딩 윈도우, RateLimiter 원자적 구현
- **[Spring Batch](https://ktae23.github.io/TIL/#batch%2Fmain%2F01-spring-batch-fundamentals.md)** (26문서) · **[Kafka](https://ktae23.github.io/TIL/#kafka%2Fmain%2F01-kafka-fundamentals.md)** (24문서) · **[Redis](https://ktae23.github.io/TIL/#redis%2Fmain%2F01-redis-fundamentals.md)** (25문서)

## 구성

```
<카테고리>/
├── main/        동작 원리
└── advanced/    실무 활용
```

문서 파일명은 `NN-kebab-case-title.md` 형식이며, 번호는 **학습 순서**입니다 (뷰어가 이 번호로 정렬).
카테고리 그룹은 [`category-groups.json`](category-groups.json)에서 관리합니다.

## 작성 도구

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) 스킬로 작성·관리합니다.

| 스킬 | 역할 |
|---|---|
| `/til` | 새 학습 기록 작성 — 카테고리 선택, 파일 생성, commit & push |
| `/til-viewer` | 전체 문서를 인터랙티브 웹 뷰어로 탐색 |
| `/curriculum` | 주제를 분석해 커리큘럼을 설계하고, 서브에이전트 팀으로 문서를 병렬 작성 |

뷰어는 [GitHub Actions](.github/workflows/deploy-til-viewer.yml)로 `main` 푸시마다 GitHub Pages에 자동 배포됩니다.

---

📫 [linkedin.com/in/ktae23](https://www.linkedin.com/in/ktae23)
