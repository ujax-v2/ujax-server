package com.ujax.global.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 페이지네이션 응답 래퍼
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

	private final List<T> content;
	private final PageInfo page;

	public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
		return new PageResponse<>(content, new PageInfo(page, size, totalElements, totalPages));
	}

	public static <T> PageResponse<T> of(List<T> content, PageInfo pageInfo) {
		return new PageResponse<>(content, pageInfo);
	}

	@Getter
	@AllArgsConstructor
	public static class PageInfo {
		private final int page;
		private final int size;
		private final long totalElements;
		private final int totalPages;

		public boolean hasNext() {
			return page < totalPages - 1;
		}

		public boolean hasPrevious() {
			return page > 0;
		}

		public boolean isFirst() {
			return page == 0;
		}

		public boolean isLast() {
			return page >= totalPages - 1;
		}
	}
}
