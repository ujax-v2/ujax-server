package com.ujax.domain.board;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class BoardCommentRepositoryTest {

	@Autowired
	private BoardCommentRepository boardCommentRepository;

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("게시글 기준으로 댓글 페이지를 조회할 수 있다")
	void findByBoardId() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board = createBoard(workspace, author);
		boardCommentRepository.save(BoardComment.create(board, author, "댓글 1"));
		boardCommentRepository.save(BoardComment.create(board, author, "댓글 2"));

		// when
		Page<BoardComment> result = boardCommentRepository.findByBoard_Id(board.getId(), PageRequest.of(0, 10));

		// then
		assertThat(result.getContent()).hasSize(2)
			.extracting("content")
			.containsExactlyInAnyOrder("댓글 1", "댓글 2");
	}

	@Test
	@DisplayName("게시글별 댓글 수를 집계할 수 있다")
	void countByBoardIds() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board1 = createBoard(workspace, author);
		Board board2 = createBoard(workspace, author);
		boardCommentRepository.save(BoardComment.create(board1, author, "댓글 1"));
		boardCommentRepository.save(BoardComment.create(board1, author, "댓글 2"));
		boardCommentRepository.save(BoardComment.create(board2, author, "댓글 3"));

		// when
		List<Object[]> rows = boardCommentRepository.countByBoardIds(List.of(board1.getId(), board2.getId()));
		Map<Long, Long> countMap = rows.stream()
			.collect(Collectors.toMap(
				row -> (Long)row[0],
				row -> ((Number)row[1]).longValue()
			));

		// then
		assertThat(countMap).containsEntry(board1.getId(), 2L);
		assertThat(countMap).containsEntry(board2.getId(), 1L);
	}

	@Test
	@DisplayName("게시글의 댓글을 일괄 삭제 상태로 변경할 수 있다")
	void softDeleteByBoardId() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board = createBoard(workspace, author);
		BoardComment comment = boardCommentRepository.save(BoardComment.create(board, author, "댓글"));

		// when
		int updated = boardCommentRepository.softDeleteByBoardId(board.getId());
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(updated).isEqualTo(1);
		Long deletedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM board_comments WHERE id = ? AND deleted_at IS NOT NULL",
			Long.class,
			comment.getId()
		);
		assertThat(deletedCount).isEqualTo(1L);
	}

	@Test
	@DisplayName("게시글과 댓글 ID로 댓글을 조회할 수 있다")
	void findByIdAndBoardId() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember author = createMember(workspace);
		Board board = createBoard(workspace, author);
		BoardComment comment = boardCommentRepository.save(BoardComment.create(board, author, "댓글"));

		// when
		BoardComment found = boardCommentRepository.findByIdAndBoardId(comment.getId(), board.getId()).orElseThrow();

		// then
		assertThat(found).extracting("id", "board.id", "content")
			.containsExactly(comment.getId(), board.getId(), "댓글");
	}

	private Workspace createWorkspace() {
		return workspaceRepository.save(Workspace.create("워크스페이스-" + UUID.randomUUID(), "소개"));
	}

	private WorkspaceMember createMember(Workspace workspace) {
		User user = userRepository.save(User.createLocalUser(UUID.randomUUID() + "@example.com", Password.ofEncoded("password"), "사용자"));
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));
	}

	private Board createBoard(Workspace workspace, WorkspaceMember author) {
		return boardRepository.save(Board.create(
			workspace,
			author,
			BoardType.FREE,
			false,
			"제목",
			"내용"
		));
	}
}
