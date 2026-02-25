package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.exception.common.InvalidParameterException;

class ProblemBoxTest {

	private final Workspace workspace = Workspace.create("워크스페이스", "소개");
	private final User user = User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저");
	private final WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER);

	@Nested
	@DisplayName("문제집 생성")
	class Create {

		@Test
		@DisplayName("문제집을 생성한다")
		void create() {
			// when
			ProblemBox problemBox = ProblemBox.create(workspace, member, "제목", "설명");

			// then
			assertThat(problemBox).extracting("title", "description")
				.containsExactly("제목", "설명");
		}

		@Test
		@DisplayName("제목이 30자를 초과하면 오류가 발생한다")
		void createWithTooLongTitle() {
			// given
			String longTitle = "a".repeat(31);

			// when & then
			assertThatThrownBy(() -> ProblemBox.create(workspace, member, longTitle, "설명"))
				.isInstanceOf(InvalidParameterException.class);
		}

		@Test
		@DisplayName("설명이 255자를 초과하면 오류가 발생한다")
		void createWithTooLongDescription() {
			// given
			String longDescription = "a".repeat(256);

			// when & then
			assertThatThrownBy(() -> ProblemBox.create(workspace, member, "제목", longDescription))
				.isInstanceOf(InvalidParameterException.class);
		}
	}

	@Nested
	@DisplayName("문제집 수정")
	class Update {

		@Test
		@DisplayName("제목과 설명을 수정한다")
		void update() {
			// given
			ProblemBox problemBox = ProblemBox.create(workspace, member, "원래 제목", "원래 설명");

			// when
			problemBox.update("새 제목", "새 설명");

			// then
			assertThat(problemBox).extracting("title", "description")
				.containsExactly("새 제목", "새 설명");
		}

		@Test
		@DisplayName("제목이 30자를 초과하면 오류가 발생한다")
		void updateWithTooLongTitle() {
			// given
			ProblemBox problemBox = ProblemBox.create(workspace, member, "제목", "설명");
			String longTitle = "a".repeat(31);

			// when & then
			assertThatThrownBy(() -> problemBox.update(longTitle, "설명"))
				.isInstanceOf(InvalidParameterException.class);
		}

		@Test
		@DisplayName("설명이 255자를 초과하면 오류가 발생한다")
		void updateWithTooLongDescription() {
			// given
			ProblemBox problemBox = ProblemBox.create(workspace, member, "제목", "설명");
			String longDescription = "a".repeat(256);

			// when & then
			assertThatThrownBy(() -> problemBox.update("제목", longDescription))
				.isInstanceOf(InvalidParameterException.class);
		}
	}
}
