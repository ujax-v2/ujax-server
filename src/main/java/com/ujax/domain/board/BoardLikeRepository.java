package com.ujax.domain.board;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLikeId> {

	@Query("SELECT bl.board.id, COUNT(bl) FROM BoardLike bl WHERE bl.board.id IN :boardIds AND bl.deleted = false GROUP BY bl.board.id")
	List<Object[]> countByBoardIds(@Param("boardIds") List<Long> boardIds);

	@Query(
		"SELECT bl.board.id FROM BoardLike bl " +
		"WHERE bl.board.id IN :boardIds AND bl.workspaceMember.id = :workspaceMemberId AND bl.deleted = false"
	)
	List<Long> findMyLikedBoardIds(
		@Param("boardIds") List<Long> boardIds,
		@Param("workspaceMemberId") Long workspaceMemberId
	);

	@Modifying
	@Query("UPDATE BoardLike bl SET bl.deleted = :deleted WHERE bl.board.id = :boardId AND bl.workspaceMember.id = :workspaceMemberId")
	int updateDeleted(
		@Param("boardId") Long boardId,
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("deleted") boolean deleted
	);

	@Modifying
	@Query("UPDATE BoardLike bl SET bl.deleted = true WHERE bl.board.id = :boardId")
	int markDeletedByBoardId(@Param("boardId") Long boardId);
}
