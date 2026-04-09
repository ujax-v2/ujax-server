# Mail Outbox / Scheduler 구조 정리

## 1. 관련 패키지 구조

```text
com.ujax.application.mail
├─ MailNotifier
├─ outbox
│  ├─ MailOutboxDeliveryProperties
│  ├─ MailOutboxEventLogger
│  ├─ MailOutboxService
│  ├─ MailSender
│  ├─ OutboxMailNotifier
│  ├─ handler
│  │  ├─ MailOutboxHandler
│  │  ├─ SignupVerificationMailOutboxHandler
│  │  └─ WorkspaceInviteMailOutboxHandler
│  └─ message
│     ├─ PreparedMailMessage
│     ├─ SignupVerificationMailPayload
│     └─ WorkspaceInviteMailPayload
└─ template
   ├─ MailTemplateLayout
   ├─ RenderedMailContent
   ├─ SignupVerificationMailTemplateRenderer
   └─ WorkspaceInviteMailTemplateRenderer

com.ujax.infrastructure
├─ config
│  ├─ SchedulingConfig
│  └─ mail
│     └─ MailOutboxSchedulerProperties
├─ scheduling
│  └─ mail
│     └─ MailOutboxScheduler
└─ external
   └─ mail
      └─ SmtpMailSender
```

## 2. 전체 흐름

```text
MailNotifier
 -> OutboxMailNotifier
 -> MailOutboxScheduler
 -> MailOutboxService
 -> MailOutboxHandler(타입별)
 -> MailTemplateRenderer(타입별)
 -> MailSender
 -> SmtpMailSender
```

### 단계별 흐름

1. 서비스 계층이 `MailNotifier` 를 호출한다.
2. `OutboxMailNotifier` 가 메일 요청을 `mail_outbox` 테이블에 적재한다.
3. `MailOutboxScheduler` 가 주기적으로 due outbox 를 조회한다.
4. `MailOutboxService` 가 PROCESSING 선점, 전송, 재시도, 실패 처리를 담당한다.
5. `MailOutboxHandler` 가 `MailType` 에 맞는 payload 를 해석해 `PreparedMailMessage` 로 변환한다.
6. 각 템플릿 렌더러가 plain text / html 본문을 만든다.
7. `MailSender` 포트를 통해 실제 발송을 요청한다.
8. `SmtpMailSender` 가 JavaMailSender 로 SMTP 전송을 수행한다.

## 3. 클래스별 책임

| 계층/역할 | 클래스 | 책임 | 몰라야 하는 것 |
|---|---|---|---|
| 진입 포트 | `MailNotifier` | 메일 요청 인터페이스 | SMTP 구현, 배치 처리 |
| outbox 적재 | `OutboxMailNotifier` | 메일 요청을 outbox row 로 저장 | 실제 발송 방식 |
| 배치 오케스트레이션 | `MailOutboxService` | 예약, 복구, 전송, 재시도 제어 | 타입별 템플릿 내용 |
| 타입별 변환 포트 | `MailOutboxHandler` | payload 를 발송 가능한 메시지로 변환 | 스케줄링, SMTP |
| 회원가입 변환 | `SignupVerificationMailOutboxHandler` | signup payload 파싱 + 제목 생성 + signup 템플릿 호출 | 다른 메일 타입 |
| 초대 변환 | `WorkspaceInviteMailOutboxHandler` | invite payload 파싱 + 제목 생성 + invite 템플릿 호출 | 다른 메일 타입 |
| 발송 메시지 DTO | `PreparedMailMessage` | subject + content 전달 | 발송 방법 |
| 텍스트/HTML DTO | `RenderedMailContent` | plain/html 본문 전달 | SMTP, 재시도 |
| 공통 레이아웃 | `MailTemplateLayout` | 공통 HTML wrapper / escape | 메일 타입별 비즈니스 데이터 |
| 회원가입 템플릿 | `SignupVerificationMailTemplateRenderer` | 회원가입 인증 메일 본문 생성 | 초대 메일 내용 |
| 초대 템플릿 | `WorkspaceInviteMailTemplateRenderer` | 워크스페이스 초대 메일 본문 생성 | 회원가입 메일 내용 |
| 발송 포트 | `MailSender` | 실제 메일 발송 추상화 | SMTP 세부 구현 |
| SMTP 구현 | `SmtpMailSender` | JavaMail 기반 SMTP 발송, 예외 로깅/변환 | outbox 재시도 정책 |
| 스케줄 실행기 | `MailOutboxScheduler` | 주기적으로 outbox batch 실행 | 템플릿 상세 |
| 스케줄 설정 | `MailOutboxSchedulerProperties` | batch-size 설정 바인딩 | 발송 로직 |
| 재시도 설정 | `MailOutboxDeliveryProperties` | retry/stuck/maxAttempts 설정 바인딩 | SMTP 구현 |

## 4. 핸들러와 템플릿의 대응 관계

현재 구조에서는 메일 타입별로 아래 대응을 가진다.

```text
MailType 1개
  -> MailOutboxHandler 1개
  -> MailTemplateRenderer 1개
```

예시:

- `SIGNUP_VERIFICATION`
  - `SignupVerificationMailOutboxHandler`
  - `SignupVerificationMailTemplateRenderer`
- `WORKSPACE_INVITE`
  - `WorkspaceInviteMailOutboxHandler`
  - `WorkspaceInviteMailTemplateRenderer`

이 구조를 쓰는 이유는 메일 종류마다 보통 아래가 함께 달라지기 때문이다.

1. payload 구조
2. subject 생성 방식
3. template 선택 및 본문 내용

## 5. 설정 값 흐름

```text
.env
 -> docker-compose.prod.yml
 -> application.yml
 -> @ConfigurationProperties / @Value
 -> Scheduler / Service
```

메일 outbox 관련 주요 env:

- `MAIL_OUTBOX_SCHEDULER_ENABLED`
- `MAIL_OUTBOX_SCHEDULER_FIXED_DELAY_MILLIS`
- `MAIL_OUTBOX_SCHEDULER_BATCH_SIZE`
- `MAIL_OUTBOX_RETRY_DELAY_MINUTES`
- `MAIL_OUTBOX_STUCK_PROCESSING_MINUTES`
- `MAIL_OUTBOX_MAX_ATTEMPTS`

## 6. 새 메일 타입 추가 시 체크리스트

1. `MailType` 추가
2. payload record 추가
3. `MailOutboxHandler` 구현체 추가
4. 템플릿 renderer 추가
5. 필요하면 `MailNotifier` / `OutboxMailNotifier` 진입 메서드 추가
6. 테스트 추가

