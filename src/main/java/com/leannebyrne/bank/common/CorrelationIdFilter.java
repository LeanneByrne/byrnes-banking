package com.leannebyrne.bank.common;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Returns the current trace id as a response header so a caller (or a
 * support agent relaying a customer's report) can quote it back, and so it
 * can be grepped for across the structured JSON logs this same request
 * produced -- the trace id IS the correlation id, rather than this project
 * hand-rolling a second, separate identifier for the same purpose.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Trace-Id";

    private final Tracer tracer;

    public CorrelationIdFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            response.setHeader(HEADER_NAME, currentSpan.context().traceId());
        }
        filterChain.doFilter(request, response);
    }
}
