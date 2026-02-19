package com.ujax.infrastructure.network;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NetworkConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
