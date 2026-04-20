package cn.jack.exam.config;

import cn.jack.exam.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        String traceNo = TraceContext.resolveTraceNo(request.getHeader(TraceContext.TRACE_NO_HEADER));
        long startedAt = System.currentTimeMillis();

        TraceContext.setTraceNo(traceNo);
        wrappedResponse.setHeader(TraceContext.TRACE_NO_HEADER, traceNo);

        try {
            log.info("traceNo={} event=request_started method={} uri={} query={}",
                    traceNo,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString() == null ? "" : request.getQueryString());

            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            String requestBody = LogSanitizer.summarizeBody(wrappedRequest.getContentAsByteArray(), wrappedRequest.getContentType());
            String responseBody = LogSanitizer.summarizeBody(wrappedResponse.getContentAsByteArray(), wrappedResponse.getContentType());

            log.info("traceNo={} event=request_completed method={} uri={} status={} durationMs={} requestBody={} responseBody={}",
                    traceNo,
                    request.getMethod(),
                    request.getRequestURI(),
                    wrappedResponse.getStatus(),
                    durationMs,
                    requestBody,
                    responseBody);

            wrappedResponse.copyBodyToResponse();
            TraceContext.clear();
        }
    }
}
