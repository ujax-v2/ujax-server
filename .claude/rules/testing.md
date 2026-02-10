# 테스트 규칙

## 테스트 파일 위치
- 도메인 단위: `domain/{도메인}/EntityTest.java` — 순수 단위 테스트
- Repository: `domain/{도메인}/RepositoryTest.java` — `@DataJpaTest` + `@ActiveProfiles("test")`
- Service: `application/{도메인}/ServiceTest.java` — `@SpringBootTest` + `@ActiveProfiles("test")`
- Controller: `infrastructure/web/api/{도메인}/ControllerTest.java` — `@WebMvcTest` (동작 검증)
- Controller Docs: `infrastructure/web/api/{도메인}/ControllerDocsTest.java` — `@WebMvcTest` + `@AutoConfigureRestDocs` + `@Tag("restDocs")` (API 문서 생성)

## Repository 테스트 원칙
- 성공 케이스만 검증 — JPA 프레임워크 동작(조회 결과 없음, `existsBy` false 등)은 테스트하지 않음
- `@Nested` 사용하지 않음 — 메서드별 flat 구조로 작성
- `@DataJpaTest` + `@ActiveProfiles("test")` + `@Import(JpaAuditingConfig.class)`

## Service 테스트 원칙
- 기본: `@SpringBootTest` + `@Autowired` + `@BeforeEach`에서 `deleteAllInBatch()`로 데이터 정리
- 실제 DB와 트랜잭션을 통해 동작을 검증 (Mockito 사용하지 않음)
- Mocking은 외부 의존이 꼭 필요한 경우에만 사용 (예: MailService, 외부 API 클라이언트)
  - 이 경우에만 `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` 허용

## Controller 테스트 vs Docs 테스트 분리

### ControllerTest (동작 검증)
- Validation 실패, 예외 처리, 엣지 케이스 검증에 집중
- Validation 테스트 시 해당 Request DTO의 `@Valid` 어노테이션을 기준으로 케이스 작성 (예: `@Size(max = 30)` → 30자 초과 테스트)
- `@Tag("restDocs")` 붙이지 않음

### ControllerDocsTest (API 문서 생성)
- Happy path 위주로 요청/응답 스펙 문서화
- `@Tag("restDocs")` + `@AutoConfigureRestDocs`
- `resource()` 빌더로 tag, summary, description, schema, fields 정의

## 테스트 스타일
- `@Nested` + `@DisplayName`으로 기능별 그룹핑 (한글 DisplayName)
- `@Nested` 안에 테스트가 1개뿐이면 `@Nested`를 사용하지 않고 flat 구조로 작성
- `@DisplayName`에 구현 디테일(클래스명 등)을 넣지 않음 — 행위 중심으로 작성
  ```
  // Bad:  "존재하지 않는 유저를 조회하면 NotFoundException이 발생한다"
  // Good: "존재하지 않는 유저를 조회하면 오류가 발생한다"
  ```
- `// given` / `// when` / `// then` 주석으로 구분
- AssertJ 사용 (`assertThat`)
- 성공 케이스 + 실패 케이스(예외 발생) 모두 검증

## 검증(assert) 스타일
- 여러 필드를 검증할 때 `assertThat`을 반복하지 않고 `extracting` + `containsExactly`로 묶어서 검증
  ```java
  // Bad
  assertThat(user.getEmail()).isEqualTo(email);
  assertThat(user.getName()).isEqualTo(name);
  assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);

  // Good
  assertThat(user).extracting("email", "name", "provider")
      .containsExactly(email, name, AuthProvider.GOOGLE);
  ```
- 컬렉션 내부 객체 검증 시 `extracting` + `containsExactlyInAnyOrder` + `tuple` 사용
  ```java
  assertThat(products).hasSize(3)
      .extracting("name", "price")
      .containsExactlyInAnyOrder(
          tuple("상품A", 1000),
          tuple("상품B", 2000),
          tuple("상품C", 3000)
      );
  ```
- 단일 필드 검증은 `extracting` 쓰지 않고 개별 `assertThat` 사용 — `extracting`은 2개 이상 필드를 묶을 때만 사용

## RestDocs
- `com.epages.restdocs-api-spec` 기반
- `@Tag("restDocs")`로 문서화 테스트 분류
- `./gradlew restDocsTest`로 별도 실행
