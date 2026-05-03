package com.petshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PayPalConfig {

    @Value("${paypal.mode}")
    private String mode;

    @Bean
    public RestClient payPalRestClient() {
        String baseUrl = "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}