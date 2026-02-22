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
class BoardLikeRepositoryTest {

	@Autowired
	private BoardLikeRepository boardLikeRepository;

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
	@DisplayName("countByBoardIds: 게시글 목록을 기준으로 삭제되지 않은 좋아요 수만 집계한다")
	void countByBoardIds() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember member1 = createMember(workspace);
		WorkspaceMember member2 = createMember(workspace);
		Board board1 = createBoard(workspace, member1);
		Board board2 = createBoard(workspace, member1);

		boardLikeRepository.save(BoardLike.create(board1, member1));
		BoardLike deletedLike = boardLikeRepository.save(BoardLike.create(board1, member2));
		deletedLike.updateDeleted(true);
		boardLikeRepository.save(BoardLike.create(board2, member1));

		// when
		List<Object[]> rows = boardLikeRepository.countByBoardIds(List.of(board1.getId(), board2.getId()));
		Map<Long, Long> countMap = rows.stream()
			.collect(Collectors.toMap(
				row -> (Long)row[0],
				row -> ((Number)row[1]).longValue()
			));

		// then
		assertThat(countMap).containsEntry(board1.getId(), 1L);
		assertThat(countMap).containsEntry(board2.getId(), 1L);
	}

	@Test
	@DisplayName("findMyLikedBoardIds: 특정 멤버가 좋아요한 게시글 ID만 조회한다")
	void findMyLikedBoardIds() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember member = createMember(workspace);
		Board board1 = createBoard(workspace, member);
		Board board2 = createBoard(workspace, member);

		boardLikeRepository.save(BoardLike.create(board1, member));
		BoardLike deletedLike = boardLikeRepository.save(BoardLike.create(board2, member));
		deletedLike.updateDeleted(true);

		// when
		List<Long> likedIds = boardLikeRepository.findMyLikedBoardIds(List.of(board1.getId(), board2.getId()), member.getId());

		// then
		assertThat(likedIds).containsExactly(board1.getId());
	}

	@Test
	@DisplayName("updateDeleted: 게시글과 멤버로 특정 좋아요 레코드의 삭제 상태를 변경한다")
	void updateDeleted() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember member = createMember(workspace);
		Board board = createBoard(workspace, member);
		boardLikeRepository.save(BoardLike.create(board, member));

		// when
		int updated = boardLikeRepository.updateDeleted(board.getId(), member.getId(), true);
		entityManager.flush();
		entityManager.clear();
		BoardLike found = boardLikeRepository.findById(new BoardLikeId(board.getId(), member.getId())).orElseThrow();

		// then
		assertThat(updated).isEqualTo(1);
		assertThat(found.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("markDeletedByBoardId: 게시글의 모든 좋아요를 삭제 상태로 일괄 변경한다")
	void markDeletedByBoardId() {
		// given
		Workspace workspace = createWorkspace();
		WorkspaceMember member1 = createMember(workspace);
		WorkspaceMember member2 = createMember(workspace);
		Board board = createBoard(workspace, member1);
		boardLikeRepository.save(BoardLike.create(board, member1));
		boardLikeRepository.save(BoardLike.create(board, member2));

		// when
		int updated = boardLikeRepository.markDeletedByBoardId(board.getId());
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(updated).isEqualTo(2);
		BoardLike like1 = boardLikeRepository.findById(new BoardLikeId(board.getId(), member1.getId())).orElseThrow();
		BoardLike like2 = boardLikeRepository.findById(new BoardLikeId(board.getId(), member2.getId())).orElseThrow();
		assertThat(like1.isDeleted()).isTrue();
		assertThat(like2.isDeleted()).isTrue();
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
