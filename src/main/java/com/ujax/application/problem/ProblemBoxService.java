package com.ujax.application.problem;

import static com.ujax.domain.problem.ProblemBox.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.problem.dto.response.ProblemBoxListItemResponse;
import com.ujax.application.problem.dto.response.ProblemBoxResponse;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.CreateProblemBoxRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateProblemBoxRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemBoxService {

	private final ProblemBoxRepository problemBoxRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public PageResponse<ProblemBoxListItemResponse> listProblemBoxes(Long workspaceId, Long userId, int page,
		int size) {
		findWorkspaceMember(workspaceId, userId);

		Page<ProblemBox> problemBoxes = problemBoxRepository.findByWorkspace_IdOrderByUpdatedAtDescIdDesc(
			workspaceId, PageRequest.of(page, size));

		List<ProblemBoxListItemResponse> content = problemBoxes.getContent().stream()
			.map(ProblemBoxListItemResponse::from)
			.toList();

		return PageResponse.of(content, problemBoxes);
	}

	public ProblemBoxResponse getProblemBox(Long workspaceId, Long problemBoxId, Long userId) {
		findWorkspaceMember(workspaceId, userId);

		ProblemBox problemBox = findProblemBox(problemBoxId, workspaceId);
		return ProblemBoxResponse.from(problemBox);
	}

	@Transactional
	public ProblemBoxResponse createProblemBox(Long workspaceId, Long userId, CreateProblemBoxRequest request) {
		WorkspaceMember member = findManagerOrOwner(workspaceId, userId);
		ProblemBox problemBox = create(member.getWorkspace(), request.title(), request.description());
		problemBoxRepository.save(problemBox);
		return ProblemBoxResponse.from(problemBox);
	}

	@Transactional
	public ProblemBoxResponse updateProblemBox(Long workspaceId, Long problemBoxId, Long userId,
		UpdateProblemBoxRequest request) {
		findManagerOrOwner(workspaceId, userId);

		ProblemBox problemBox = findProblemBox(problemBoxId, workspaceId);
		problemBox.update(request.title(), request.description());
		return ProblemBoxResponse.from(problemBox);
	}

	@Transactional
	public void deleteProblemBox(Long workspaceId, Long problemBoxId, Long userId) {
		findManagerOrOwner(workspaceId, userId);

		ProblemBox problemBox = findProblemBox(problemBoxId, workspaceId);
		problemBoxRepository.delete(problemBox);
	}

	private WorkspaceMember findManagerOrOwner(Long workspaceId, Long userId) {
		WorkspaceMember member = findWorkspaceMember(workspaceId, userId);
		member.validateManagerOrOwner();
		return member;
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private ProblemBox findProblemBox(Long problemBoxId, Long workspaceId) {
		return problemBoxRepository.findByIdAndWorkspace_Id(problemBoxId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_BOX_NOT_FOUND));
	}
}
