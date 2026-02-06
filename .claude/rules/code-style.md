# 코드 스타일 규칙

## Javadoc / 주석 — 최소주의 원칙
주석은 코드만으로 알 수 없는 "왜(why)" 또는 "주의사항"만 작성한다.

### 작성하지 않는 것
- 메서드 Javadoc — 메서드명으로 의도를 드러냄 (`getUser`, `createOAuthUser` 등)
- 이름만으로 충분한 필드 (`email`, `name`, `profileImageUrl` 등)
- getter/setter, 팩토리 메서드, CRUD 메서드에 대한 설명
- 클래스/인터페이스에 "XXX 서비스", "XXX 레포지토리" 같은 이름 반복 Javadoc
- Enum 각 상수별 Javadoc — 텍스트 필드가 대체함

### 작성하는 것
- 필드가 **조건에 따라 null**이 되는 경우: 인라인 `/** 한줄 설명 */`
  ```java
  /** 비밀번호 (OAuth 사용 시 null) */
  private String password;

  /** OAuth 식별자 (자체 회원가입 시 null) */
  private String providerId;
  ```
- **기술적 이유가 있는 우회/오버라이드**: 왜 그렇게 했는지 한 줄 설명
  ```java
  /** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
  @Override
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findById(@Param("id") Long id);
  ```
- **비즈니스 맥락**이 코드에서 드러나지 않을 때: 클래스 레벨 한 줄 Javadoc
  ```java
  /** API 공통 응답 래퍼 */
  public class ApiResponse<T> { }
  ```

## Enum
- `@Getter` + `@RequiredArgsConstructor` 사용
- 각 상수에 한글 설명 텍스트 필드 보유 (예: `GOOGLE("구글 회원가입")`)
- 개별 상수에 Javadoc 달지 않음 — 텍스트 필드가 설명 역할을 대신함

## Entity
- `BaseEntity` 상속 (createdAt, updatedAt, deletedAt)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- `@Builder`는 private 생성자에 선언 (클래스 레벨 아님)
- 정적 팩토리 메서드로 생성 (예: `createOAuthUser`, `createLocalUser`)
- Soft Delete: `@SQLDelete` + `@Filter(name = "softDeleteFilter")`
- 불변 필드에는 setter/update 메서드를 만들지 않음

## Repository
- `findById`는 JPQL `@Query`로 오버라이드 필수 — `EntityManager.find()`는 `@Filter`가 적용되지 않으므로 JPQL로 변환해야 soft delete 필터가 동작함
  ```java
  @Override
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findById(@Param("id") Long id);
  ```
- 엔티티가 늘어나면 Custom Base Repository(`SimpleJpaRepository` 상속)로 전환 검토
- Validation: `@Valid` 사용 (`@Validated`는 그룹 검증이 필요할 때만)

## DTO
- Request/Response는 `record` 사용
- Response에 `from(Entity)` 정적 팩토리 메서드로 변환

## Validation 계층 분리
- **Request DTO**: 파라미터 존재 여부만 검증 (`@NotNull`, `@NotBlank` 등 필수값 체크)
- **Domain (Entity/Service)**: 값의 제약 및 비즈니스 규칙 검증 (길이, 범위, 중복, 잔액 부족 등)
- `@Size`, `@Min`, `@Max` 같은 값 제약은 도메인 책임 — Request DTO에 넣지 않음

## Service
- 클래스 레벨 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`
- `@RequiredArgsConstructor`로 생성자 주입
- 조회 실패 시 커스텀 예외 사용 (예: `NotFoundException(ErrorCode.USER_NOT_FOUND)`)

## Controller
- `@RestController` + `@RequestMapping("/api/v1/{resource}")` 형식
- `@RequiredArgsConstructor`로 생성자 주입
- 공통 래퍼 `ApiResponse`, `PageResponse` 사용
