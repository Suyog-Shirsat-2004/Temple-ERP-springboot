package com.temple.erp.security;

import com.temple.erp.model.User;
import com.temple.erp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TempleUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, TempleUserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtService.parse(token);
            Object isAdmin = claims.get("isAdmin");
            String id = claims.get("id", String.class);
            AuthPrincipal principal;
            if (Boolean.TRUE.equals(isAdmin) || "true".equals(String.valueOf(isAdmin)) || "admin".equals(id)) {
                principal = userDetailsService.hardcodedAdminPrincipal();
            } else {
                Optional<User> userOpt = userRepository.findById(id);
                if (userOpt.isEmpty() || !"active".equalsIgnoreCase(userOpt.get().getStatus())) {
                    chain.doFilter(request, response);
                    return;
                }
                principal = userDetailsService.principalForUser(userOpt.get());
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ignored) {
            // Invalid token: proceed unauthenticated; secured endpoints will reject.
        }
        chain.doFilter(request, response);
    }
}
