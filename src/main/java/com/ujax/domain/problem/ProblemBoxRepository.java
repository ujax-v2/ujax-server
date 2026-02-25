package com.ujax.domain.problem;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemBoxRepository extends JpaRepository<ProblemBox, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT pb FROM ProblemBox pb WHERE pb.id = :id")
	Optional<ProblemBox> findById(@Param("id") @NonNull Long id);

	Page<ProblemBox> findByWorkspace_IdOrderByUpdatedAtDescIdDesc(Long workspaceId, Pageable pageable);

	Optional<ProblemBox> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
