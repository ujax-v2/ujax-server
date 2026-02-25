package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BusinessRuleViolationException;

class WorkspaceJoinRequestTest {

	@Nested
	@DisplayName("가입 신청 생성")
	class CreateJoinRequest {

		@Test
		@DisplayName("가입 신청을 생성하면 상태는 PENDING이다")
		void createJoinRequestPending() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("join-request@example.com", Password.ofEncoded("password"), "신청자");

			// when
			WorkspaceJoinRequest request = WorkspaceJoinRequest.create(workspace, user);

			// then
			assertThat(request).extracting("workspace", "user", "status")
				.containsExactly(workspace, user, WorkspaceJoinRequestStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("가입 신청 상태 변경")
	class UpdateJoinRequestStatus {

		@Test
		@DisplayName("대기중 신청을 수락할 수 있다")
		void approveJoinRequest() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("approve@example.com", Password.ofEncoded("password"), "신청자");
			WorkspaceJoinRequest request = WorkspaceJoinRequest.create(workspace, user);

			// when
			request.approve();

			// then
			assertThat(request.getStatus()).isEqualTo(WorkspaceJoinRequestStatus.APPROVED);
		}

		@Test
		@DisplayName("이미 처리된 신청은 다시 처리할 수 없다")
		void updateProcessedJoinRequestThrowsException() {
			// given
			Workspace workspace = Workspace.create("워크스페이스", "소개");
			User user = User.createLocalUser("processed@example.com", Password.ofEncoded("password"), "신청자");
			WorkspaceJoinRequest request = WorkspaceJoinRequest.create(workspace, user);
			request.approve();

			// when & then
			assertThatThrownBy(request::reject)
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_JOIN_REQUEST_ALREADY_PROCESSED);
		}
	}
}
