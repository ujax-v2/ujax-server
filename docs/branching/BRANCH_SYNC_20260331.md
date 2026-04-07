# Branch Sync 2026-03-31

## 현재 상태

- `main` 최신 커밋: `6adadf4`
- `origin/main` 최신 커밋: `6adadf4`
- 이번 반영 범위: `#80`, `#79`, `#81`
- 현재 `develop`는 기존 히스토리를 유지하고 있음
- 롤백 대비 백업 브랜치 유지:
  - `backup/origin-main-20260331`
  - `backup/origin-develop-20260331`

## 이번에 수행한 작업

1. `origin/main`과 `origin/develop`의 공통 조상, 커밋 차이, 파일 차이를 확인했다.
2. `develop`의 실제 코드 상태는 유지하면서 꼬인 히스토리를 정리할 수 있도록 `develop-clean-20260331`를 `origin/main` 기준으로 새로 만들었다.
3. 아래 3개 변경만 `cherry-pick -x`로 재적용했다.
   - `db2b9f0` `#80`
   - `0f0726f` `#79`
   - `e4bac07` `#81`
4. `develop-clean-20260331`와 기존 `origin/develop`가 파일 기준 동일한지 확인했다.
5. `./gradlew test`를 실행해 통과를 확인했다.
6. `main-release-20260331`를 `origin/main`에서 만들고, `develop-clean-20260331`를 `--ff-only`로 올렸다.
7. `main` 보호 규칙 해제 후 `main-release-20260331 -> main` fast-forward push를 수행했다.
8. 작업 종료 후 임시 브랜치를 정리했다.
   - 삭제: `main-release-20260331`
   - 삭제: `develop-clean-20260331`
   - 삭제: `tmp/develop-bridge-sim`
   - 삭제: `docs/update-openapi3-from-develop`
   - 원격 삭제: `main-release-20260331`
   - 원격 삭제: `develop-clean-20260331`

## 왜 이렇게 처리했는가

- 기존 `develop`는 실제 파일 내용은 문제 없었지만, `main`과의 머지 이력이 squash와 역병합 때문에 비정상적으로 꼬여 있었다.
- 이 상태에서 `develop -> main`을 그대로 진행하면 `main` 히스토리도 계속 지저분해진다.
- 그래서 이번에는 `main` 위에 실제 필요한 변경만 다시 쌓은 clean release 경로를 만들어 `main`에 반영했다.
- `develop`는 팀 작업 연속성을 위해 그대로 유지했다.

## 이제부터의 진행 방식

1. `develop`는 계속 통합 브랜치로 사용한다.
2. 당분간 `develop -> main` 직접 merge 또는 PR은 하지 않는다.
3. 다음 릴리스가 필요하면 `origin/main`에서 새 release 브랜치를 만든다.
4. 그 release 브랜치에 이번 배포 대상 커밋만 선별 적용한다.
5. 검증 후 `main`에는 fast-forward로 반영한다.

## 권장 운영 규칙

- feature 브랜치는 계속 `develop` 기준으로 만든다.
- `develop -> main` 직접 merge 금지
- release는 항상 `main` 기준 clean release 브랜치로 진행
- 장기 브랜치 사이의 `squash merge`와 `rebase merge`는 피한다.
- `main` 보호 규칙을 다시 원래 수준으로 복구한다.

## 다음 릴리스 절차 예시

```bash
git fetch origin
git switch -c main-release-YYYYMMDD origin/main
# 필요한 커밋만 cherry-pick
./gradlew test
git push -u origin main-release-YYYYMMDD
```

보호 규칙상 direct push가 불가능하면 PR로 진행하고, 같은 히스토리가 꼭 필요하면 관리자 승인 하에 fast-forward push 여부를 별도로 결정한다.

## 롤백 기준

- `main` 이전 기준점: `3047557`
- 현재 반영 커밋: `6adadf4`
- 백업 브랜치:
  - `backup/origin-main-20260331`
  - `backup/origin-develop-20260331`
