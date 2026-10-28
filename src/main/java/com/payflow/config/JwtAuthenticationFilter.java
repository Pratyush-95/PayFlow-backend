
package com.payflow.config;

import com.payflow.security.JwtService;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            if (!jwtService.isTokenValid(token)) {
                sendJsonError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired JWT token"
                );
                return;
            }

            Claims claims = jwtService.extractAllClaims(token);

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || role == null || role.isBlank()) {
                sendJsonError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid JWT claims"
                );
                return;
            }

            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();

            sendJsonError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired JWT token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendJsonError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":" + status
                        + ",\"error\":\"Unauthorized\""
                        + ",\"message\":\"" + message + "\"}"
        );
    }
}
