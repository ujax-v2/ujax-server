package com.ujax.infrastructure.external.judge0;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

class Judge0ConfigTest {

    private final Judge0Config judge0Config = new Judge0Config();

    @Test
    @DisplayName("Judge0 전용 RestTemplate을 생성한다")
    void createJudge0RestTemplate() {
        // when
        RestTemplate restTemplate = judge0Config.judge0RestTemplate();

        // then
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
    }
}
