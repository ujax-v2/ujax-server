package com.ujax.application.board;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.board.dto.response.BoardDetailResponse;
import com.ujax.application.board.dto.response.BoardListItemResponse;
import com.ujax.application.board.dto.response.BoardListResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardCommentRepository;
import com.ujax.domain.board.BoardLikeRepository;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.board.BoardType;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.dto.PageResponse.PageInfo;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

	private static final int TITLE_MIN = 1;
	private static final int TITLE_MAX = 50;
	private static final int CONTENT_MIN = 1;
	private static final int CONTENT_MAX = 2000;
	private static final int PREVIEW_LIMIT = 100;

	private final BoardRepository boardRepository;
	private final BoardCommentRepository boardCommentRepository;
	private final BoardLikeRepository boardLikeRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public BoardListResponse listBoards(
		Long workspaceId,
		Long workspaceMemberId,
		BoardType type,
		String keyword,
		int page,
		int size,
		String sort,
		boolean pinnedFirst
	) {
		validateMember(workspaceId, workspaceMemberId);
		validatePageable(page, size);

		String searchKeyword = normalizeKeyword(keyword);
		Sort sortSpec = buildSort(sort, pinnedFirst);
		PageRequest pageable = PageRequest.of(page, size, sortSpec);
		Page<Board> result = boardRepository.search(workspaceId, type, searchKeyword, pageable);

		List<Long> boardIds = result.getContent().stream().map(Board::getId).toList();
		Map<Long, Long> commentCounts = boardIds.isEmpty()
			? Map.of()
			: toCountMap(boardCommentRepository.countByBoardIds(boardIds));
		Map<Long, Long> likeCounts = boardIds.isEmpty()
			? Map.of()
			: toCountMap(boardLikeRepository.countByBoardIds(boardIds));
		List<Long> myLikedIds = boardIds.isEmpty()
			? List.of()
			: boardLikeRepository.findMyLikedBoardIds(boardIds, workspaceMemberId);

		List<BoardListItemResponse> items = result.getContent().stream()
			.map(board -> BoardListItemResponse.from(
				board,
				preview(board.getContent()),
				likeCounts.getOrDefault(board.getId(), 0L),
				commentCounts.getOrDefault(board.getId(), 0L),
				myLikedIds.contains(board.getId())
			))
			.toList();

		return BoardListResponse.of(
			items,
			new PageInfo(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
		);
	}

	@Transactional
	public BoardDetailResponse getBoardDetail(Long workspaceId, Long boardId, Long workspaceMemberId) {
		validateMember(workspaceId, workspaceMemberId);

		Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
		boardRepository.incrementViewCount(workspaceId, boardId);

		long commentCount = boardCommentRepository.countByBoard_Id(board.getId());
		long likeCount = extractSingleCount(boardLikeRepository.countByBoardIds(List.of(board.getId())));
		boolean myLike = !boardLikeRepository.findMyLikedBoardIds(List.of(board.getId()), workspaceMemberId).isEmpty();

		return BoardDetailResponse.from(board, likeCount, commentCount, myLike);
	}

	@Transactional
	public BoardDetailResponse createBoard(Long workspaceId, Long workspaceMemberId, BoardType type, String title, String content, Boolean pinned) {
		Workspace workspace = findWorkspaceById(workspaceId);
		WorkspaceMember author = findWorkspaceMember(workspaceId, workspaceMemberId);

		validateTitle(title);
		validateContent(content);

		boolean pinnedValue = pinned != null && pinned;
		Board board = Board.create(workspace, author, type, pinnedValue, title, content);
		Board saved = boardRepository.save(board);

		return BoardDetailResponse.from(saved, 0L, 0L, false);
	}

	@Transactional
	public BoardDetailResponse updateBoard(
		Long workspaceId,
		Long boardId,
		Long workspaceMemberId,
		BoardType type,
		String title,
		String content,
		Boolean pinned
	) {
		WorkspaceMember actor = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);

		if (type == null && title == null && content == null && pinned == null) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		if (title != null) {
			validateTitle(title);
		}
		if (content != null) {
			validateContent(content);
		}

		board.update(type, title, content, pinned);

		long commentCount = boardCommentRepository.countByBoard_Id(board.getId());
		long likeCount = extractSingleCount(boardLikeRepository.countByBoardIds(List.of(board.getId())));
		boolean myLike = !boardLikeRepository.findMyLikedBoardIds(List.of(board.getId()), workspaceMemberId).isEmpty();

		return BoardDetailResponse.from(board, likeCount, commentCount, myLike);
	}

	@Transactional
	public void pinBoard(Long workspaceId, Long boardId, Long workspaceMemberId, boolean pinned) {
		WorkspaceMember actor = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);
		board.updatePinned(pinned);
	}

	@Transactional
	public void deleteBoard(Long workspaceId, Long boardId, Long workspaceMemberId) {
		WorkspaceMember actor = validateMember(workspaceId, workspaceMemberId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);

		boardLikeRepository.softDeleteByBoardId(board.getId());
		boardCommentRepository.softDeleteByBoardId(board.getId());
		boardRepository.delete(board);
	}

	private Workspace findWorkspaceById(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private Board findBoard(Long workspaceId, Long boardId) {
		return boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long workspaceMemberId) {
		return workspaceMemberRepository.findByWorkspace_IdAndId(workspaceId, workspaceMemberId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "워크스페이스에 소속된 멤버가 아닙니다."));
	}

	private WorkspaceMember validateMember(Long workspaceId, Long workspaceMemberId) {
		return findWorkspaceMember(workspaceId, workspaceMemberId);
	}

	private void validateAuthor(WorkspaceMember actor, Board board) {
		if (!board.getAuthor().getId().equals(actor.getId())) {
			throw new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "작성자만 이 작업을 수행할 수 있습니다.");
		}
	}

	private void validateTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		int length = title.length();
		if (length < TITLE_MIN || length > TITLE_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		int length = content.length();
		if (length < CONTENT_MIN || length > CONTENT_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validatePageable(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword;
	}

	private Sort buildSort(String sort, boolean pinnedFirst) {
		Sort base = parseSort(sort);
		if (!pinnedFirst) {
			return base;
		}
		return Sort.by(Sort.Order.desc("pinned")).and(base);
	}

	private Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Order.desc("createdAt"));
		}
		String[] parts = sort.split(",");
		if (parts.length != 2) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
		String property = parts[0].trim();
		String direction = parts[1].trim();
		if (!isAllowedSortProperty(property)) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
		Sort.Direction dir;
		try {
			dir = Sort.Direction.fromString(direction);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
		return Sort.by(new Sort.Order(dir, property));
	}

	private boolean isAllowedSortProperty(String property) {
		return "createdAt".equals(property)
			|| "updatedAt".equals(property)
			|| "viewCount".equals(property);
	}

	private String preview(String content) {
		if (content == null) {
			return "";
		}
		if (content.length() <= PREVIEW_LIMIT) {
			return content;
		}
		return content.substring(0, PREVIEW_LIMIT);
	}

	private Map<Long, Long> toCountMap(List<Object[]> results) {
		Map<Long, Long> map = new HashMap<>();
		for (Object[] row : results) {
			Long boardId = (Long)row[0];
			Long count = (Long)row[1];
			map.put(boardId, count);
		}
		return map;
	}

	private long extractSingleCount(List<Object[]> results) {
		if (results.isEmpty()) {
			return 0L;
		}
		return (Long)results.get(0)[1];
	}
}
