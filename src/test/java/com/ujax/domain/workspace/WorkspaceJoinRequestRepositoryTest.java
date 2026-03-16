package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class WorkspaceJoinRequestRepositoryTest {

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceJoinRequestRepository workspaceJoinRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("워크스페이스와 유저로 가입 신청 존재 여부를 확인할 수 있다")
	void existsByWorkspaceIdAndUserId() {
		// given
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		User user = userRepository.save(
			User.createLocalUser("join-request-repo@example.com", Password.ofEncoded("password"), "신청자")
		);
		workspaceJoinRequestRepository.save(WorkspaceJoinRequest.create(workspace, user));

		// when
		boolean exists = workspaceJoinRequestRepository.existsByWorkspace_IdAndUser_Id(workspace.getId(), user.getId());

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("워크스페이스별 가입 신청을 페이지 조회할 수 있다")
	void findByWorkspaceIdOrderByCreatedAtDesc() {
		// given
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		User user = userRepository.save(
			User.createLocalUser("join-request-page@example.com", Password.ofEncoded("password"), "신청자")
		);
		WorkspaceJoinRequest saved = workspaceJoinRequestRepository.save(WorkspaceJoinRequest.create(workspace, user));

		// when
		var page = workspaceJoinRequestRepository.findByWorkspace_IdOrderByCreatedAtDesc(
			workspace.getId(),
			org.springframework.data.domain.PageRequest.of(0, 20)
		);

		// then
		assertThat(page.getContent())
			.extracting("id")
			.containsExactly(saved.getId());
	}
}
