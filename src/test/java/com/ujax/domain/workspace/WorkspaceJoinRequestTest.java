package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;

class WorkspaceJoinRequestTest {

	@Nested
	@DisplayName("가입 신청 생성")
	class CreateJoinRequest {

		@Test
		@DisplayName("가입 신청을 생성하면 워크스페이스와 사용자가 설정된다")
		void createJoinRequest() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("join-request@example.com", Password.ofEncoded("password"), "신청자");

			// when
			WorkspaceJoinRequest request = WorkspaceJoinRequest.create(workspace, user);

			// then
			assertThat(request).extracting("workspace", "user")
				.containsExactly(workspace, user);
		}
	}
}
