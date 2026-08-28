package com.ujax.infrastructure.external.judge0;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Judge0Config {

    @Bean("judge0RestTemplate")
    public RestTemplate judge0RestTemplate() {
        return new RestTemplate();
    }
}
