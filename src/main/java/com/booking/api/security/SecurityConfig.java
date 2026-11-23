package com.booking.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtService jwtService,
                        UserDetailsService userDetailsService,
                        JsonSecurityHandlers handlers) throws Exception {
                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/rooms").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint(handlers)
                                                .accessDeniedHandler(handlers))
                                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                                                UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        @Bean
        public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                return new ProviderManager(provider);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
