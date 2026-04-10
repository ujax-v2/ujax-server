package com.ujax.infrastructure.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SchedulingConfigTest {

	@Test
	@DisplayName("스케줄러는 설정한 pool size 와 thread prefix 를 사용한다")
	void taskSchedulerUsesConfiguredPoolSize() {
		SchedulingConfig schedulingConfig = new SchedulingConfig();

		ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler)schedulingConfig.taskScheduler(4);

		assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
		assertThat(taskScheduler.getThreadNamePrefix()).isEqualTo("ujax-scheduler-");
		taskScheduler.destroy();
	}
}
