package com.petshop.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(
        @DefaultValue("BLOCK") SessionPolicy policy,
        @DefaultValue("2m") Duration inactivityTimeout
) {
}