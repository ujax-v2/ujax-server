package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class WorkspaceMemberRepositoryTest {

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("워크스페이스와 유저로 멤버를 조회할 수 있다")
	void findByWorkspaceIdAndUserId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", "password", "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));

		// when
		WorkspaceMember member = workspaceMemberRepository
			.findByWorkspace_IdAndUser_Id(workspace.getId(), user.getId())
			.orElseThrow();

		// then
		assertThat(member).extracting("workspace.id", "user.id")
			.containsExactly(workspace.getId(), user.getId());
	}

	@Test
	@DisplayName("워크스페이스 멤버 목록을 조회할 수 있다")
	void findByWorkspaceId() {
		// given
		User user = userRepository.save(User.createLocalUser("list@example.com", "password", "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));

		// when
		var result = workspaceMemberRepository.findByWorkspace_Id(workspace.getId());

		// then
		assertThat(result).hasSize(1);
	}
}
