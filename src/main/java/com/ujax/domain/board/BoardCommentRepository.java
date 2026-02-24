package com.ujax.domain.board;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ujax.infrastructure.persistence.jpa.IncludeDeleted;
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT bc FROM BoardComment bc WHERE bc.id = :id")
	Optional<BoardComment> findById(@Param("id") @NonNull Long id);

	@EntityGraph(attributePaths = {"board", "author"})
	@IncludeDeleted
	@Query(
		"""
		SELECT bc FROM BoardComment bc
		WHERE bc.board.id = :boardId
		AND bc.deletedAt IS NULL
		AND bc.board.deletedAt IS NULL
		AND bc.board.workspace.deletedAt IS NULL
		"""
	)
	Page<BoardComment> findByBoard_Id(@Param("boardId") Long boardId, Pageable pageable);

	long countByBoard_Id(Long boardId);

	@EntityGraph(attributePaths = {"board", "author"})
	@IncludeDeleted
	@Query(
		"""
		SELECT bc FROM BoardComment bc
		WHERE bc.id = :commentId
		AND bc.board.id = :boardId
		AND bc.deletedAt IS NULL
		AND bc.board.deletedAt IS NULL
		AND bc.board.workspace.deletedAt IS NULL
		"""
	)
	Optional<BoardComment> findByIdAndBoardId(
		@Param("commentId") Long commentId,
		@Param("boardId") Long boardId
	);

	@Query("SELECT bc.board.id, COUNT(bc) FROM BoardComment bc WHERE bc.board.id IN :boardIds GROUP BY bc.board.id")
	List<Object[]> countByBoardIds(@Param("boardIds") List<Long> boardIds);

	@Modifying
	@Query("UPDATE BoardComment bc SET bc.deletedAt = CURRENT_TIMESTAMP WHERE bc.board.id = :boardId")
	int softDeleteByBoardId(@Param("boardId") Long boardId);
}
