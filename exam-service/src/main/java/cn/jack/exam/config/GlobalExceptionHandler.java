package cn.jack.exam.config;

import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException exception) {
        log.warn("traceNo={} event=unauthorized_exception exceptionType={} message={}",
                TraceContext.getTraceNo(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(ForbiddenException exception) {
        log.warn("traceNo={} event=forbidden_exception exceptionType={} message={}",
                TraceContext.getTraceNo(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "Invalid request"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("traceNo={} event=validation_exception exceptionType={} message={}",
                TraceContext.getTraceNo(),
                exception.getClass().getSimpleName(),
                message);
        return Map.of("message", exception.getBindingResult().getFieldError() == null
                ? "Invalid request"
                : exception.getBindingResult().getFieldError().getDefaultMessage());
    }
}
