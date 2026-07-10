package com.moneyMagnetApi.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${pluggy.base-url}")
    private String pluggyBaseUrl;
    
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl(pluggyBaseUrl)
                .build();
    }
}
