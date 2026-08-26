# UJAX Architecture

UJAX는 프론트엔드, API 서버, 코드 실행 환경과 백준 제출용 Chrome Extension을 분리합니다. 서버가 사용자의 백준 로그인 정보를 보관하지 않으면서도 웹 IDE의 코드 실행과 실제 온라인 저지 제출을 하나의 흐름으로 연결하기 위한 구조입니다.

## 시스템 아키텍처

```mermaid
flowchart TB
    User[사용자 브라우저]
    Front[React · Vite · Monaco Editor]
    Extension[UJAX Chrome Extension]
    Server[Spring Boot API]
    DB[(MySQL)]
    Redis[(Redis)]
    Judge[Judge0 Workers]
    BOJ[Baekjoon Online Judge]
    S3[AWS S3]
    OAuth[Google · Kakao OAuth2]
    Mail[SMTP]
    MM[Mattermost Webhook]
    Metrics[Prometheus · Loki · Grafana]

    User --> Front
    Front -->|REST / JWT| Server
    Front <-->|Bridge handshake| Extension
    Extension -->|브라우저 로그인 세션| BOJ
    Server --> DB
    Server --> Redis
    Server -->|코드 실행·polling| Judge
    Server --> S3
    Server --> OAuth
    Server --> Mail
    Server --> MM
    Server --> Metrics
```

## 주요 요청 흐름

### Judge0 코드 실행

```mermaid
sequenceDiagram
    actor U as 사용자
    participant F as Web IDE
    participant S as UJAX Server
    participant J as Judge0

    U->>F: 코드 실행
    F->>S: 코드·언어·테스트케이스 전송
    S->>J: submission 생성
    J-->>S: token 반환
    loop 채점 완료까지
        S->>J: 상태 polling
        J-->>S: status·stdout·stderr
    end
    S-->>F: 테스트 결과 반환
    F-->>U: 케이스별 결과 표시
```

### 백준 제출

```mermaid
sequenceDiagram
    actor U as 사용자
    participant F as UJAX Front
    participant E as Chrome Extension
    participant B as Baekjoon

    U->>F: 제출 요청
    F->>E: 문제·언어·코드 전달
    E->>E: 연결·인증 상태 확인
    E->>B: 로그인된 브라우저에서 제출 페이지 열기
    E-->>U: 코드 입력과 제출 단계 안내
    B-->>E: 채점 상태
    E-->>F: 제출 결과 전달
```

### 명세 동기화

```mermaid
flowchart LR
    Test[REST Docs Test] --> OpenAPI[openapi3.yaml]
    OpenAPI --> Spec[ujax-api-spec]
    Spec --> Types[TypeScript types]
    Types --> Front[ujax-front]
```

## 핵심 ERD

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ WORKSPACE_MEMBER : joins
    USER ||--o{ WORKSPACE_JOIN_REQUEST : requests
    WORKSPACE ||--o{ WORKSPACE_MEMBER : has
    WORKSPACE ||--o{ WORKSPACE_JOIN_REQUEST : receives
    WORKSPACE ||--o{ PROBLEM_BOX : owns
    WORKSPACE ||--o{ BOARD : contains
    PROBLEM_BOX ||--o{ WORKSPACE_PROBLEM : contains
    PROBLEM ||--o{ SAMPLE : has
    PROBLEM ||--o{ WORKSPACE_PROBLEM : assigned_as
    PROBLEM }o--o{ ALGORITHM_TAG : tagged
    WORKSPACE_PROBLEM ||--o{ SOLUTION : receives
    WORKSPACE_MEMBER ||--o{ SOLUTION : writes
    SOLUTION ||--o{ SOLUTION_COMMENT : has
    SOLUTION ||--o{ SOLUTION_LIKE : receives
    WORKSPACE_MEMBER ||--o{ SOLUTION_COMMENT : writes
    WORKSPACE_MEMBER ||--o{ SOLUTION_LIKE : reacts
    WORKSPACE_MEMBER ||--o{ BOARD : writes
    BOARD ||--o{ BOARD_COMMENT : has
    BOARD ||--o{ BOARD_LIKE : receives
    WORKSPACE_MEMBER ||--o{ BOARD_COMMENT : writes
    WORKSPACE_MEMBER ||--o{ BOARD_LIKE : reacts
```

ERD는 현재 JPA 엔티티의 연관관계를 기준으로 핵심 협업 도메인만 표현했습니다. 이메일 Outbox와 웹훅 알림 로그처럼 독립적으로 재시도·감사 이력을 관리하는 운영 테이블은 가독성을 위해 제외했습니다.

## 유즈케이스

```mermaid
flowchart LR
    Guest[방문자]
    Member[스터디 구성원]
    Manager[운영자 / 관리자]

    Guest --> Auth[회원가입·이메일 인증·OAuth 로그인]
    Guest --> Explore[워크스페이스 탐색·가입 신청]

    Member --> Dashboard[공지·마감 문제·통계 조회]
    Member --> IDE[문제 풀이·Judge0 실행]
    Member --> Submit[Extension으로 백준 제출]
    Member --> Solution[풀이 버전·댓글·좋아요]
    Member --> Community[게시글·댓글·좋아요]

    Manager --> Workspace[워크스페이스 설정·멤버 관리]
    Manager --> Problems[문제집·문제·마감일 관리]
    Manager --> Notify[초대 메일·Mattermost 알림 설정]
```

## 저장소 경계

- [`ujax-server`](https://github.com/ujax-v2/ujax-server): 도메인 규칙, API, 인증, 실행·알림 연동
- [`ujax-front`](https://github.com/ujax-v2/ujax-front): 사용자 화면과 클라이언트 상태
- [`ujax-extension`](https://github.com/ujax-v2/ujax-extension): 백준 브라우저 세션을 사용하는 제출 연결
- [`ujax-api-spec`](https://github.com/ujax-v2/ujax-api-spec): 서버와 프론트가 공유하는 API 계약
