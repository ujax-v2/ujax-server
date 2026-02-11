package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
			User user = User.createLocalUser("test@example.com", "password", "유저");
			WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);

			// when
			member.updateRole(WorkspaceMemberRole.MANAGER);

			// then
			assertThat(member.getRole()).isEqualTo(WorkspaceMemberRole.MANAGER);
		}
	}
}
