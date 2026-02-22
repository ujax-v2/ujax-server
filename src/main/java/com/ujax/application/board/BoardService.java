package com.ujax.application.board;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.board.dto.request.BoardCreateRequest;
import com.ujax.application.board.dto.request.BoardListRequest;
import com.ujax.application.board.dto.request.BoardUpdateRequest;
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
import com.ujax.domain.workspace.WorkspaceMemberRole;
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
		Long userId,
		BoardListRequest request
	) {
		BoardType type = request.type();
		String keyword = request.keyword();
		int page = request.page();
		int size = request.size();
		String sort = request.sort();
		boolean pinnedFirst = request.pinnedFirst();

		WorkspaceMember viewer = validateMember(workspaceId, userId);
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
		Set<Long> myLikedBoardIds = boardIds.isEmpty()
			? Set.of()
			: Set.copyOf(boardLikeRepository.findMyLikedBoardIds(boardIds, viewer.getId()));
		List<BoardListItemResponse> items = result.getContent().stream()
			.map(board -> BoardListItemResponse.from(
				board,
				preview(board.getContent()),
				likeCounts.getOrDefault(board.getId(), 0L),
				commentCounts.getOrDefault(board.getId(), 0L),
				myLikedBoardIds.contains(board.getId())
			))
			.toList();

		return BoardListResponse.of(
			items,
			new PageInfo(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
		);
	}

	@Transactional
	public BoardDetailResponse getBoardDetail(Long workspaceId, Long boardId, Long userId) {
		WorkspaceMember viewer = validateMember(workspaceId, userId);

		Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
		boardRepository.incrementViewCount(workspaceId, boardId);
		long commentCount = boardCommentRepository.countByBoard_Id(boardId);
		long likeCount = extractSingleCount(boardLikeRepository.countByBoardIds(List.of(boardId)));
		boolean myLike = !boardLikeRepository.findMyLikedBoardIds(List.of(boardId), viewer.getId()).isEmpty();

		return BoardDetailResponse.from(board, board.getViewCount() + 1L, likeCount, commentCount, myLike);
	}

	@Transactional
	public BoardDetailResponse createBoard(Long workspaceId, Long userId, BoardCreateRequest request) {
		Workspace workspace = findWorkspaceById(workspaceId);
		WorkspaceMember author = findWorkspaceMember(workspaceId, userId);

		String title = request.title();
		String content = request.content();

		validateNoticePermission(author, request.type());
		validateTitle(title);
		validateContent(content);

		boolean pinnedValue = request.pinned() != null && request.pinned();
		validatePinnedAllowed(request.type(), pinnedValue);
		Board board = Board.create(workspace, author, request.type(), pinnedValue, title, content);
		Board saved = boardRepository.save(board);

		return BoardDetailResponse.from(saved, 0L, 0L, false);
	}

	@Transactional
	public BoardDetailResponse updateBoard(
		Long workspaceId,
		Long boardId,
		Long userId,
		BoardUpdateRequest request
	) {
		WorkspaceMember actor = validateMember(workspaceId, userId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);

		String title = request.title();
		String content = request.content();

		if (request.type() == null && title == null && content == null && request.pinned() == null) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		if (title != null) {
			validateTitle(title);
		}
		if (content != null) {
			validateContent(content);
		}
		BoardType nextType = request.type() != null ? request.type() : board.getType();
		boolean nextPinned = request.pinned() != null ? request.pinned() : board.isPinned();
		validateNoticePermission(actor, nextType);
		validatePinnedAllowed(nextType, nextPinned);

		board.update(request.type(), title, content, request.pinned());

		long commentCount = boardCommentRepository.countByBoard_Id(board.getId());
		long likeCount = extractSingleCount(boardLikeRepository.countByBoardIds(List.of(board.getId())));
		boolean myLike = !boardLikeRepository.findMyLikedBoardIds(List.of(board.getId()), actor.getId()).isEmpty();

		return BoardDetailResponse.from(board, likeCount, commentCount, myLike);
	}

	@Transactional
	public void pinBoard(Long workspaceId, Long boardId, Long userId, boolean pinned) {
		WorkspaceMember actor = validateMember(workspaceId, userId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);
		validatePinnedAllowed(board.getType(), pinned);
		board.updatePinned(pinned);
	}

	@Transactional
	public void deleteBoard(Long workspaceId, Long boardId, Long userId) {
		WorkspaceMember actor = validateMember(workspaceId, userId);
		Board board = findBoard(workspaceId, boardId);
		validateAuthor(actor, board);

		boardLikeRepository.markDeletedByBoardId(board.getId());
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

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "워크스페이스에 소속된 멤버가 아닙니다."));
	}

	private WorkspaceMember validateMember(Long workspaceId, Long userId) {
		return findWorkspaceMember(workspaceId, userId);
	}

	private void validateAuthor(WorkspaceMember actor, Board board) {
		if (!board.getAuthor().getId().equals(actor.getId())) {
			throw new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "작성자만 이 작업을 수행할 수 있습니다.");
		}
	}

	private void validateNoticePermission(WorkspaceMember author, BoardType type) {
		if (type == null || type != BoardType.NOTICE) {
			return;
		}
		WorkspaceMemberRole role = author.getRole();
		if (role != WorkspaceMemberRole.MANAGER && role != WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "공지 게시글은 운영자 이상만 작성할 수 있습니다.");
		}
	}

	private void validatePinnedAllowed(BoardType type, boolean pinned) {
		if (!pinned) {
			return;
		}
		if (type != BoardType.NOTICE) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
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
