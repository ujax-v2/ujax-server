# ujax-server

Spring Boot 3.5 / Java 21 / Gradle 기반 백엔드 프로젝트

## 패키지 구조
```
com.ujax
├── domain/           # 엔티티, Repository 인터페이스, enum
├── application/      # Service, Response DTO
├── infrastructure/
│   ├── web/          # Controller, Request DTO
│   └── persistence/  # JPA 설정, AOP
└── global/           # 예외, 공통 설정
```

## 빌드 & 검증 명령어
- `./gradlew verify` : editorconfig + test + jacoco (LINE 70%)
- `./gradlew restDocsTest` : RestDocs 기반 OpenAPI 스펙 생성
- `./gradlew test` : 전체 테스트

## 기술 스택
- DB: MySQL (prod) / H2 (test), Flyway 마이그레이션
- Soft Delete: `@SQLDelete` + `@Filter(name = "softDeleteFilter")` + `BaseEntity`
- API 문서: restdocs-api-spec → OpenAPI 3.0 → swagger-ui.html
- JaCoCo 제외: QueryDSL(Q*), Application, Config, dto, external, global
