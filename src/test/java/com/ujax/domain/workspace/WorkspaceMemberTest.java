package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;

class WorkspaceMemberTest {

	@Nested
	@DisplayName("워크스페이스 멤버 권한 수정")
	class UpdateRole {

		@Test
		@DisplayName("권한을 변경할 수 있다")
		void updateRole() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저");
			WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);

			// when
			member.updateRole(WorkspaceMemberRole.MANAGER);

			// then
			assertThat(member.getRole()).isEqualTo(WorkspaceMemberRole.MANAGER);
		}
	}

	@Nested
	@DisplayName("워크스페이스 멤버 활동 기록")
	class RecordActivity {

		@Test
		@DisplayName("연속된 날짜에 활동하면 스트릭이 증가한다")
		void recordActivity() {
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저");
			WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);

			member.recordActivity(LocalDate.of(2026, 3, 20));
			member.recordActivity(LocalDate.of(2026, 3, 21));
			member.recordActivity(LocalDate.of(2026, 3, 21));
			member.recordActivity(LocalDate.of(2026, 3, 22));

			assertThat(member.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 3, 22));
			assertThat(member.getCurrentStreakDays()).isEqualTo(3);
		}

		@Test
		@DisplayName("오늘 또는 어제 활동 기준으로 현재 스트릭을 반환한다")
		void getCurrentStreakDays() {
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저");
			WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);

			member.recordActivity(LocalDate.of(2026, 3, 20));
			member.recordActivity(LocalDate.of(2026, 3, 21));
			member.recordActivity(LocalDate.of(2026, 3, 22));

			assertThat(member.getCurrentStreakDays(LocalDate.of(2026, 3, 22))).isEqualTo(3);
			assertThat(member.getCurrentStreakDays(LocalDate.of(2026, 3, 23))).isEqualTo(3);
			assertThat(member.getCurrentStreakDays(LocalDate.of(2026, 3, 24))).isZero();
		}
	}
}
