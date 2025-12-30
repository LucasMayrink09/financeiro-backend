package com.gestao.financeira.security;

import com.gestao.financeira.entity.User;
import com.gestao.financeira.repository.UserRepository;
import com.gestao.financeira.service.JwtService;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);

            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                if (!user.isEnabled()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Conta desativada ou banida.");
                    return;
                }

                long tokenPwdChange = jwtService.extractPwdChange(token);
                long userPwdChange = user.getLastPasswordChange() != null
                        ? user.getLastPasswordChange().toEpochMilli()
                        : 0L;

                if (tokenPwdChange != userPwdChange) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado por alteração de credenciais");
                    return;
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null,
                            user.getRoles().stream().map(SimpleGrantedAuthority::new).toList()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            logger.error("Erro ao processar autenticação JWT", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token inválido ou expirado\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
