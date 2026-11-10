package com.temple.erp.config;

import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/admin/login", "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/passes/types", "/api/content/**", "/api/bookings/rooms", "/api/restaurant/menu", "/api/donations/total").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/passes/*").permitAll() // pass by pass_id (public in original route)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                }))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        AuthenticationSuccessHandler successHandler = (request, response, authentication) -> {
            String loginType = request.getParameter("loginType");
            boolean adminLoginPage = "admin".equalsIgnoreCase(loginType);
            boolean isAdmin = false;
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthPrincipal ap) {
                isAdmin = ap.isAdmin();
            } else {
                isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            }

            // Admins must use the admin login page only (show generic wrong-information error on user page)
            if (!adminLoginPage && isAdmin) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?error=wrong");
                return;
            }

            // Admin login page is only for admin accounts (show generic wrong-information error)
            if (adminLoginPage && !isAdmin) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/admin/login?error=wrong");
                return;
            }

            String target = isAdmin ? "/admin/dashboard" : "/dashboard";
            response.sendRedirect(request.getContextPath() + target);
        };

        AuthenticationFailureHandler failureHandler = (request, response, exception) -> {
            String loginType = request.getParameter("loginType");
            if ("admin".equalsIgnoreCase(loginType)) {
                response.sendRedirect(request.getContextPath() + "/admin/login?error=true");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?error=true");
            }
        };

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/login", "/admin/login", "/register", "/forgot-password",
                                "/about", "/services", "/temple-updates",
                                "/events", "/festivals", "/news", "/passes/verify/**", "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/favicon.svg", "/uploads/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll())
                .headers(Customizer.withDefaults());
        return http.build();
    }
}
