package com.boilerplate.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The extracted services do not yet issue or validate a shared identity. Fail closed: only
 * health probes are reachable until the identity/JWT trust boundary is implemented.
 */
@Configuration
public class GatewaySecurityConfiguration {

    @Bean
    SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .build();
    }
}
