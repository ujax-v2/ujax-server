package com.ujax.domain.board;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.User;
import com.ujax.domain.user.Password;
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
class BoardRepositoryTest {

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("워크스페이스 기준으로 게시글 단건을 조회할 수 있다")
	void findByIdAndWorkspaceId() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board = boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.FREE,
			false,
			"제목",
			"내용"
		));

		// when
		Board found = boardRepository.findByIdAndWorkspaceId(board.getId(), workspace.getId()).orElseThrow();

		// then
		assertThat(found).extracting("id", "workspace.id", "author.id", "title")
			.containsExactly(board.getId(), workspace.getId(), author.getId(), "제목");
	}

	@Test
	@DisplayName("타입과 키워드로 게시글을 검색할 수 있다")
	void searchByTypeAndKeyword() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board target = boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.QNA,
			false,
			"질문 제목",
			"검색 가능한 본문"
		));
		boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.FREE,
			false,
			"일반 제목",
			"일반 본문"
		));

		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));

		// when
		Page<Board> result = boardRepository.search(workspace.getId(), BoardType.QNA, "검색", pageable);

		// then
		assertThat(result.getContent())
			.extracting("id")
			.containsExactly(target.getId());
	}

	@Test
	@DisplayName("조회수를 증가시킬 수 있다")
	void incrementViewCount() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board = boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.FREE,
			false,
			"제목",
			"내용"
		));

		// when
		int updated = boardRepository.incrementViewCount(workspace.getId(), board.getId());
		entityManager.flush();
		entityManager.clear();
		Board found = boardRepository.findById(board.getId()).orElseThrow();

		// then
		assertThat(updated).isEqualTo(1);
		assertThat(found.getViewCount()).isEqualTo(1L);
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace) {
		User user = userRepository.save(User.createLocalUser(UUID.randomUUID() + "@example.com", Password.ofEncoded("password"), "사용자"));
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));
	}
}
