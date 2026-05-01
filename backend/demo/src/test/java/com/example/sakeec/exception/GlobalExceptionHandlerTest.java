package com.example.sakeec.exception;

import com.example.sakeec.dto.ConflictErrorResponse;
import com.example.sakeec.dto.ErrorResponse;
import com.example.sakeec.dto.ValidationErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setMdc() {
        MDC.put("traceId", "test-trace-1234");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("NotFoundException → 404 + errorCode=NOT_FOUND + traceId 同梱")
    void notFound() {
        ResponseEntity<ErrorResponse> res = handler.handleNotFound(new NotFoundException("missing"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("NOT_FOUND");
        assertThat(body.traceId()).isEqualTo("test-trace-1234");
        assertThat(body.message()).isEqualTo("missing");
    }

    @Test
    @DisplayName("OutOfStockException → 409 + errorCode=OUT_OF_STOCK")
    void outOfStock() {
        ResponseEntity<ErrorResponse> res = handler.handleOutOfStock(new OutOfStockException());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("InvalidStatusTransitionException → 409 + currentStatus を含む ConflictErrorResponse")
    void invalidStatusTransition() {
        ResponseEntity<ConflictErrorResponse> res = handler.handleInvalidTransition(
                new InvalidStatusTransitionException("DELIVERED", "SHIPPED", "DELIVERED"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ConflictErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(body.currentStatus()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("BusinessException (汎用) → 400 + 元の errorCode 維持 (ログイン失敗 AUTH_FAILED など)")
    void businessException() {
        ResponseEntity<ErrorResponse> res = handler.handleBusiness(
                new BusinessException("AUTH_FAILED", "ユーザー名またはパスワードが正しくありません"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("AUTH_FAILED");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + フィールド別エラー一覧")
    void validation() throws Exception {
        Object target = new Object();
        BindingResult br = new BeanPropertyBindingResult(target, "obj");
        // rejectValue ではなく FieldError を直接 addError する方が
        // ターゲットのプロパティ getter に依存せず確実
        br.addError(new FieldError("obj", "email", "正しいメール形式ではありません"));
        br.addError(new FieldError("obj", "name",  "必須です"));

        Method dummy = this.getClass().getDeclaredMethod("dummyHandler");
        MethodParameter mp = new MethodParameter(dummy, -1);

        ResponseEntity<ValidationErrorResponse> res = handler.handleValidation(
                new MethodArgumentNotValidException(mp, br));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ValidationErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.errors()).containsKeys("email", "name");
        assertThat(body.traceId()).isEqualTo("test-trace-1234");
    }

    @Test
    @DisplayName("セキュリティ: 想定外例外 (NullPointerExceptionなど) → 500 + 固定メッセージ。" +
            "内部実装詳細 (例外メッセージ・スタック) はレスポンスに漏れない")
    void unexpectedExceptionDoesNotLeakDetails() {
        ResponseEntity<ErrorResponse> res = handler.handleGeneral(
                new NullPointerException("connection string=jdbc://secret:pw@db"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(res.getBody());
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("internal server error");
        // 内部メッセージが漏れていない
        assertThat(body.message()).doesNotContain("jdbc");
        assertThat(body.message()).doesNotContain("secret");
    }

    @SuppressWarnings("unused")
    private void dummyHandler() {}
}
