package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("이름과 소개, 웹훅을 수정할 수 있다")
		void updateAllFields() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// when
			workspace.update("새 이름", "새 소개", "https://hook.example.com");

			// then
			assertThat(workspace).extracting("name", "description", "mmWebhookUrl")
				.containsExactly("새 이름", "새 소개", "https://hook.example.com");
		}

		@Test
		@DisplayName("null 값은 기존 값을 유지한다")
		void updateIgnoreNulls() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// when
			workspace.update(null, "바뀐 소개", null);

			// then
			assertThat(workspace).extracting("name", "description", "mmWebhookUrl")
				.containsExactly("워크스페이스", "바뀐 소개", null);
		}
	}
}
