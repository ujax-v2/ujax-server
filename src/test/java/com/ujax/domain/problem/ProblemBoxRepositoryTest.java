package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ProblemBoxRepositoryTest {

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("워크스페이스 ID로 문제집 목록을 페이징 조회한다")
	void findByWorkspaceId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));

		problemBoxRepository.save(ProblemBox.create(workspace, member, "문제집1", "설명1"));
		problemBoxRepository.save(ProblemBox.create(workspace, member, "문제집2", "설명2"));

		// when
		Page<ProblemBox> result = problemBoxRepository.findByWorkspace_IdOrderByUpdatedAtDescIdDesc(
			workspace.getId(), PageRequest.of(0, 10));

		// then
		assertThat(result.getContent()).hasSize(2)
			.extracting("title")
			.containsExactly("문제집2", "문제집1");
	}

	@Test
	@DisplayName("ID와 워크스페이스 ID로 문제집을 조회한다")
	void findByIdAndWorkspaceId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		WorkspaceMember member = workspaceMemberRepository.save(
			WorkspaceMember.create(workspace, user, WorkspaceMemberRole.OWNER));
		ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, member, "문제집", "설명"));

		// when
		Optional<ProblemBox> result = problemBoxRepository.findByIdAndWorkspace_Id(saved.getId(), workspace.getId());

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).extracting("title", "description")
			.containsExactly("문제집", "설명");
	}
}
