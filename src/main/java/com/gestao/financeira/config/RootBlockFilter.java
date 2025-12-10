package com.gestao.financeira.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RootBlockFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        if (path.equals("/")) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.getWriter().write("Acesso não autorizado.");
            return;
        }

        chain.doFilter(request, response);
    }
}
