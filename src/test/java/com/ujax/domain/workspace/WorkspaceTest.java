package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

	@Nested
	@DisplayName("워크스페이스 생성")
	class CreateWorkspace {

		@Test
		@DisplayName("이미지 URL이 없으면 기본 이미지가 설정된다")
		void createWithDefaultImage() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// then
			assertThat(workspace.getImageUrl()).isEqualTo(Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}

		@Test
		@DisplayName("이미지 URL이 있으면 해당 이미지가 설정된다")
		void createWithImage() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개", "https://new-image.com/workspace.png");

			// then
			assertThat(workspace.getImageUrl()).isEqualTo("https://new-image.com/workspace.png");
		}
	}

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("이름과 소개, 웹훅, 이미지를 수정할 수 있다")
		void updateAllFields() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// when
			workspace.update("새 이름", "새 소개", "https://hook.example.com", "https://new-image.com/workspace.png");

			// then
			assertThat(workspace).extracting("name", "description", "mmWebhookUrl", "imageUrl")
				.containsExactly("새 이름", "새 소개", "https://hook.example.com", "https://new-image.com/workspace.png");
		}

		@Test
		@DisplayName("이미지만 수정할 수 있다")
		void updateOnlyImage() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// when
			workspace.update(null, null, null, "https://new-image.com/workspace.png");

			// then
			assertThat(workspace).extracting("name", "description", "mmWebhookUrl", "imageUrl")
				.containsExactly("워크스페이스", "소개", null, "https://new-image.com/workspace.png");
		}

		@Test
		@DisplayName("null 값은 기존 값을 유지한다")
		void updateIgnoreNulls() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");

			// when
			workspace.update(null, "바뀐 소개", null, null);

			// then
			assertThat(workspace).extracting("name", "description", "mmWebhookUrl", "imageUrl")
				.containsExactly("워크스페이스", "바뀐 소개", null, Workspace.DEFAULT_WORKSPACE_IMAGE_URL);
		}
	}
}
