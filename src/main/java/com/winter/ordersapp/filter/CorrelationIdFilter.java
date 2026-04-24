package com.winter.ordersapp.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CorrelationIdFilter extends OncePerRequestFilter {
@Override

    protected void doFilterInternal(HttpServletRequest request,

                                    HttpServletResponse response,

                                    FilterChain filterChain)

            throws ServletException, IOException {

        String correlationId = Optional.ofNullable(request.getHeader("X-Correlation-ID"))

                .orElse(UUID.randomUUID().toString());

        MDC.put("correlationId", correlationId);

        try {

            response.setHeader("X-Correlation-ID", correlationId);

            filterChain.doFilter(request, response);

        } finally {

            MDC.clear();

        }

    }
}
