package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class WorkspaceRepositoryTest {

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("워크스페이스 이름으로 검색할 수 있다")
	void findByNameContaining() {
		// given
		Workspace workspace1 = workspaceRepository.save(Workspace.create("테스트 공간", "소개"));
		workspaceRepository.save(Workspace.create("다른 공간", "소개"));

		// when
		Page<Workspace> result = workspaceRepository.findByNameContaining("테스트", PageRequest.of(0, 10));

		// then
		assertThat(result.getContent())
			.extracting("id")
			.containsExactly(workspace1.getId());
	}

	@Test
	@DisplayName("검색어가 없으면 최신순으로 전체 워크스페이스를 조회한다")
	void findAllLatestOrder() {
		// given
		Workspace older = workspaceRepository.save(Workspace.create("오래된 공간", "소개"));
		Workspace newer = workspaceRepository.save(Workspace.create("새 공간", "소개"));

		// when
		Page<Workspace> result = workspaceRepository.findAll(
			PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(
				org.springframework.data.domain.Sort.Order.desc("createdAt"),
				org.springframework.data.domain.Sort.Order.desc("id")
			))
		);

		// then
		assertThat(result.getContent())
			.extracting("id")
			.containsExactly(newer.getId(), older.getId());
	}

	@Test
	@DisplayName("유저가 속한 워크스페이스를 정렬 조건으로 조회할 수 있다")
	void findByMemberUserId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));

		// when
		var result = workspaceRepository.findByMemberUserId(
			user.getId(),
			Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
		);

		// then
		assertThat(result)
			.extracting("id")
			.containsExactly(workspace.getId());
	}

	@Test
	@DisplayName("유저 워크스페이스 목록을 최신순으로 정렬할 수 있다")
	void findByMemberUserIdOrderedByLatest() {
		// given
		User user = userRepository.save(User.createLocalUser("sorted@example.com", Password.ofEncoded("password"), "유저"));
		Workspace older = workspaceRepository.save(Workspace.create("오래된 워크스페이스", "소개"));
		Workspace newer = workspaceRepository.save(Workspace.create("새 워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(older, user, WorkspaceMemberRole.MEMBER));
		workspaceMemberRepository.save(WorkspaceMember.create(newer, user, WorkspaceMemberRole.MEMBER));

		// when
		var result = workspaceRepository.findByMemberUserId(
			user.getId(),
			Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
		);

		// then
		assertThat(result)
			.extracting("id")
			.containsExactly(newer.getId(), older.getId());
	}

	@Test
	@DisplayName("워크스페이스 이름 중복 여부를 확인할 수 있다")
	void existsByName() {
		// given
		workspaceRepository.save(Workspace.create("중복", "소개"));

		// when
		boolean exists = workspaceRepository.existsByName("중복");

		// then
		assertThat(exists).isTrue();
	}
}
