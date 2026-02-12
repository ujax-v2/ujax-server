package com.ujax.domain.board;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT b FROM Board b WHERE b.id = :id")
	Optional<Board> findById(@Param("id") @NonNull Long id);

	@EntityGraph(attributePaths = {"workspace", "author"})
	@Query("SELECT b FROM Board b WHERE b.id = :boardId AND b.workspace.id = :workspaceId")
	Optional<Board> findByIdAndWorkspaceId(@Param("boardId") Long boardId, @Param("workspaceId") Long workspaceId);

	boolean existsByIdAndWorkspace_Id(Long boardId, Long workspaceId);

	@EntityGraph(attributePaths = {"workspace", "author"})
	@Query(
		"""
		SELECT b FROM Board b
		WHERE b.workspace.id = :workspaceId
		AND (:type IS NULL OR b.type = :type)
		AND (:keyword IS NULL OR b.title LIKE %:keyword% OR b.content LIKE %:keyword%)
		"""
	)
	Page<Board> search(
		@Param("workspaceId") Long workspaceId,
		@Param("type") BoardType type,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@Modifying
	@Query("UPDATE Board b SET b.viewCount = b.viewCount + 1 WHERE b.workspace.id = :workspaceId AND b.id = :boardId")
	int incrementViewCount(@Param("workspaceId") Long workspaceId, @Param("boardId") Long boardId);
}
