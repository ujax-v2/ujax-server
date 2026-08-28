package com.ujax.application.metrics;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.domain.metrics.UserLoginLog;
import com.ujax.domain.metrics.UserLoginLogRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginMetricsService {

	private final UserLoginLogRepository userLoginLogRepository;
	private final MeterRegistry meterRegistry;

	/**
	 * 앱 기동 시 DAU / MAU / YAU Gauge 등록
	 * Gauge supplier는 Prometheus가 스크랩할 때마다 자동 호출됨 (별도 스케줄 불필요)
	 */
	@PostConstruct
	public void registerGauges() {
		Gauge.builder("ujax.users.active.daily", userLoginLogRepository, repo -> {
				try {
					return repo.countDau();
				} catch (Exception e) {
					log.warn("DAU 조회 실패: {}", e.getMessage());
					return 0;
				}
			})
			.description("Daily Active Users - 오늘 로그인한 서로 다른 계정 수")
			.register(meterRegistry);

		Gauge.builder("ujax.users.active.monthly", userLoginLogRepository, repo -> {
				try {
					return repo.countMau();
				} catch (Exception e) {
					log.warn("MAU 조회 실패: {}", e.getMessage());
					return 0;
				}
			})
			.description("Monthly Active Users - 이번달 로그인한 서로 다른 계정 수")
			.register(meterRegistry);

		Gauge.builder("ujax.users.active.yearly", userLoginLogRepository, repo -> {
				try {
					return repo.countYau();
				} catch (Exception e) {
					log.warn("YAU 조회 실패: {}", e.getMessage());
					return 0;
				}
			})
			.description("Yearly Active Users - 올해 로그인한 서로 다른 계정 수")
			.register(meterRegistry);

		log.info("LoginMetricsService: DAU/MAU/YAU Gauge 등록 완료");
	}

	/**
	 * 로그인 이벤트 기록
	 * AuthService에서 로그인 성공 시 호출
	 */
	@Transactional
	public void recordLogin(Long userId) {
		userLoginLogRepository.save(UserLoginLog.of(userId));
	}

	/**
	 * 오래된 로그 자동 정리 — 매월 1일 새벽 3시
	 * YAU 기준 1년 초과 데이터 삭제
	 */
	@Scheduled(cron = "0 0 3 1 * *")
	@Transactional
	public void cleanUpOldLogs() {
		LocalDateTime cutoff = LocalDateTime.now().minusYears(1);
		userLoginLogRepository.deleteOlderThan(cutoff);
		log.info("LoginMetricsService: {} 이전 로그인 로그 삭제 완료", cutoff);
	}
}
