package com.ujax.domain.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

	/**
	 * DAU: 오늘 날짜에 로그인한 서로 다른 계정 수
	 */
	@Query(value = """
		SELECT COUNT(DISTINCT user_id)
		FROM user_login_log
		WHERE DATE(logged_in_at) = CURDATE()
		""", nativeQuery = true)
	long countDau();

	/**
	 * MAU: 이번 달(캘린더 기준)에 로그인한 서로 다른 계정 수
	 */
	@Query(value = """
		SELECT COUNT(DISTINCT user_id)
		FROM user_login_log
		WHERE YEAR(logged_in_at)  = YEAR(CURDATE())
		  AND MONTH(logged_in_at) = MONTH(CURDATE())
		""", nativeQuery = true)
	long countMau();

	/**
	 * YAU: 올해(캘린더 기준)에 로그인한 서로 다른 계정 수
	 */
	@Query(value = """
		SELECT COUNT(DISTINCT user_id)
		FROM user_login_log
		WHERE YEAR(logged_in_at) = YEAR(CURDATE())
		""", nativeQuery = true)
	long countYau();

	/**
	 * 오래된 로그 삭제 — 연간 집계 기준 1년 초과 데이터 정리
	 */
	@Modifying
	@Query(value = "DELETE FROM user_login_log WHERE logged_in_at < :cutoff", nativeQuery = true)
	void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
