package cn.jack.exam.config;

import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldHandleUnauthorizedException() {
        var response = globalExceptionHandler.handleUnauthorized(new UnauthorizedException("Authentication is required"));

        assertThat(response).containsEntry("message", "Authentication is required");
    }

    @Test
    void shouldHandleForbiddenException() {
        var response = globalExceptionHandler.handleForbidden(new ForbiddenException("Permission denied"));

        assertThat(response).containsEntry("message", "Permission denied");
    }

    @Test
    void shouldHandleBadRequestException() {
        var response = globalExceptionHandler.handleBadRequest(new BadRequestException("题目不存在"));

        assertThat(response).containsEntry("message", "题目不存在");
    }

    @Test
    void shouldHandleValidationExceptionWithFieldMessage() throws Exception {
        Method method = ValidationTarget.class.getDeclaredMethod("submit", ValidationRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationRequest(), "request");
        bindingResult.addError(new FieldError("request", "name", "名称不能为空"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = globalExceptionHandler.handleValidation(exception);

        assertThat(response).containsEntry("message", "名称不能为空");
    }

    private static class ValidationTarget {
        @SuppressWarnings("unused")
        public void submit(@Valid ValidationRequest request) {
        }
    }

    private static class ValidationRequest {
        @SuppressWarnings("unused")
        private String name;
    }
}
