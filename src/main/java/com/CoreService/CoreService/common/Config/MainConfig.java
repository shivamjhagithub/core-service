package com.CoreService.CoreService.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class MainConfig {

    /**
     * Jackson is left to Spring Boot's auto-configuration so that
     * {@code spring.jackson.*} settings apply; declaring an ObjectMapper bean
     * here would silently discard them.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
