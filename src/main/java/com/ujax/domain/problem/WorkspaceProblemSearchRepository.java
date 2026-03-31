package com.ujax.domain.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkspaceProblemSearchRepository {

	Page<WorkspaceProblem> searchByProblemBoxId(Long problemBoxId, String keyword, Pageable pageable);
}
