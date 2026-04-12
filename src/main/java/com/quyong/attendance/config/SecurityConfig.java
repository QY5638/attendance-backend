package com.quyong.attendance.config;

import com.quyong.attendance.module.auth.security.BearerTokenAuthenticationFilter;
import com.quyong.attendance.module.auth.security.RestAccessDeniedHandler;
import com.quyong.attendance.module.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
                .authorizeRequests()
                .antMatchers("/api/health", "/api/auth/login", "/api/auth/refresh").permitAll()
                .antMatchers("/api/auth/logout").authenticated()
                .antMatchers("/api/face/register", "/api/face/verify", "/api/face/liveness/**")
                .hasAnyRole("ADMIN", "EMPLOYEE")
                .antMatchers("/api/attendance/checkin", "/api/attendance/device-options", "/api/attendance/record/**", "/api/attendance/repair")
                .hasAnyRole("ADMIN", "EMPLOYEE")
                .antMatchers("/api/statistics/personal", "/api/statistics/summary").hasAnyRole("ADMIN", "EMPLOYEE")
                .antMatchers("/api/attendance/list").hasRole("ADMIN")
                .antMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().authenticated();

        http.addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
