package com.elink.evco.platform.common.web;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：把所有失败路径收敛为稳定 ApiResponse 外观与登记错误码。
 *
 * <p>稳定输出规则：参数校验失败归并为 VALIDATION_ERROR；业务异常透传登记码；
 * 未预期异常归并 INTERNAL_ERROR 且不携带堆栈到响应；错误详情只进受控日志。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 受控日志；未预期异常在此记录完整堆栈供排查。 */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务失败统一转换。
     *
     * @param ex 业务异常。
     * @return 登记错误码对应的 HTTP 状态与响应体。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.errorCode().httpStatus())
                .body(
                        ApiResponse.failure(
                                ex.errorCode().code(),
                                ex.getMessage(),
                                TraceIdHolder.current().value()));
    }

    /**
     * 请求体字段校验失败（@Valid）归并为 VALIDATION_ERROR。
     *
     * @param ex 校验异常。
     * @return 400 响应，说明中携带首个失败字段与原因。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message =
                first != null
                        ? "参数校验失败：" + first.getField() + " " + first.getDefaultMessage()
                        : PlatformErrorCode.VALIDATION_ERROR.defaultMessage();
        return failure(PlatformErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * 单参数校验失败（@Validated 注解在参数上）归并为 VALIDATION_ERROR。
     *
     * @param ex 约束冲突异常。
     * @return 400 响应。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return failure(PlatformErrorCode.VALIDATION_ERROR, "参数校验失败：" + ex.getMessage());
    }

    /**
     * 缺失必填请求参数归并为 VALIDATION_ERROR。
     *
     * @param ex 缺参异常。
     * @return 400 响应。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return failure(
                PlatformErrorCode.VALIDATION_ERROR, "缺少必填参数：" + ex.getParameterName());
    }

    /**
     * 参数类型不匹配归并为 VALIDATION_ERROR。
     *
     * @param ex 类型不匹配异常。
     * @return 400 响应。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return failure(
                PlatformErrorCode.VALIDATION_ERROR,
                "参数类型不匹配：" + ex.getName());
    }

    /**
     * 请求体不可读归并为 VALIDATION_ERROR。
     *
     * @param ex 不可读异常。
     * @return 400 响应。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return failure(PlatformErrorCode.VALIDATION_ERROR, "请求体格式不合法");
    }

    /**
     * 静态资源未找到（含不存在路径）归并为 RESOURCE_NOT_FOUND，不暴露路由细节。
     *
     * @param ex 资源未找到异常。
     * @return 404 响应。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return failure(PlatformErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 未预期异常兜底：完整堆栈只进受控日志，响应统一 INTERNAL_ERROR。
     *
     * @param ex 未预期异常。
     * @return 500 响应，不含内部细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        LOG.error("未预期系统错误 traceId={}", TraceIdHolder.current().value(), ex);
        return failure(PlatformErrorCode.INTERNAL_ERROR);
    }

    /**
     * 构造失败响应的公共路径。
     *
     * @param errorCode 已登记错误码。
     * @return 统一失败响应。
     */
    private ResponseEntity<ApiResponse<Void>> failure(PlatformErrorCode errorCode) {
        return failure(errorCode, errorCode.defaultMessage());
    }

    /**
     * 构造带场景化说明的失败响应。
     *
     * @param errorCode 已登记错误码。
     * @param message 场景化中文说明。
     * @return 统一失败响应。
     */
    private ResponseEntity<ApiResponse<Void>> failure(PlatformErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(
                        ApiResponse.failure(
                                errorCode.code(), message, TraceIdHolder.current().value()));
    }
}
