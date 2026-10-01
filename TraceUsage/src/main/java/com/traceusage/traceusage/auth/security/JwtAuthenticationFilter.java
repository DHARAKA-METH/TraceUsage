package com.traceusage.traceusage.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccountStatusUserDetailsChecker accountChecker = new AccountStatusUserDetailsChecker();

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        boolean hasBearerToken = authorization != null
                && (authorization.equalsIgnoreCase("Bearer")
                || authorization.regionMatches(true, 0, "Bearer ", 0, 7));

        if (!hasBearerToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject;
        try {
            String token = authorization.length() > 6 ? authorization.substring(7).trim() : "";
            subject = jwtService.validateAccessTokenAndGetSubject(token);
        } catch (JwtException | IllegalArgumentException exception) {
            reject(response);
            return;
        }

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(subject);
            accountChecker.check(user);
        } catch (UsernameNotFoundException | AccountStatusException exception) {
            reject(response);
            return;
        }

        if (!subject.equalsIgnoreCase(user.getUsername())) {
            reject(response);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Access token is invalid or expired\",\"data\":null}");
    }
}
