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

import com.ujax.domain.workspace.Workspace;
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

	@Test
	@DisplayName("워크스페이스 ID로 문제집 목록을 페이징 조회한다")
	void findByWorkspaceId() {
		// given
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));

		problemBoxRepository.save(ProblemBox.create(workspace, "문제집1", "설명1"));
		problemBoxRepository.save(ProblemBox.create(workspace, "문제집2", "설명2"));

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
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		ProblemBox saved = problemBoxRepository.save(ProblemBox.create(workspace, "문제집", "설명"));

		// when
		Optional<ProblemBox> result = problemBoxRepository.findByIdAndWorkspace_Id(saved.getId(), workspace.getId());

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).extracting("title", "description")
			.containsExactly("문제집", "설명");
	}
}
