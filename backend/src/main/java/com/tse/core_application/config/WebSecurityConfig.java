package com.tse.core_application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/health/**").permitAll()
                // OpenAPI 3 / Springdoc paths
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // Policy endpoints
                .antMatchers("/api/orgs/*/createGeoFencingPolicy", "/api/orgs/*/getGeoFencePolicy", "/api/orgs/*/updateGeoFencePolicy", "/api/orgs/getAllGeoFencePolicies").permitAll()
                // Fence endpoints
                .antMatchers("/api/orgs/*/createFence", "/api/orgs/*/updateFence", "/api/orgs/*/getFence", "/api/allFence").permitAll()
                // Assignment endpoints
                .antMatchers("/api/orgs/*/assignFenceToEntity", "/api/orgs/*/getAssignedEntityOfFence").permitAll()
                // User Fences endpoints
                .antMatchers("/api/orgs/*/getUserFences").permitAll()
                // Punch Request endpoints
                .antMatchers("/api/orgs/*/requestPunchForEntity", "/api/orgs/*/getPendingRequest", "/api/orgs/*/getPunchRequestById", "/api/orgs/*/getPendingRequestHistory").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated();

        http.headers().frameOptions().sameOrigin();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN");
    }
}