package cn.jack.exam.config;

import org.slf4j.MDC;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TraceContext {

    public static final String TRACE_NO_HEADER = "TraceNo";
    public static final String TRACE_NO_KEY = "TraceNo";
    private static final Pattern TRACE_NO_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private TraceContext() {
    }

    public static String resolveTraceNo(String traceNoHeader) {
        if (traceNoHeader != null && !traceNoHeader.isBlank()) {
            String normalizedTraceNo = traceNoHeader.replace("-", "").toLowerCase(Locale.ROOT);
            if (TRACE_NO_PATTERN.matcher(normalizedTraceNo).matches()) {
                return normalizedTraceNo;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void setTraceNo(String traceNo) {
        MDC.put(TRACE_NO_KEY, traceNo);
    }

    public static String getTraceNo() {
        return MDC.get(TRACE_NO_KEY);
    }

    public static void clear() {
        MDC.remove(TRACE_NO_KEY);
    }
}
