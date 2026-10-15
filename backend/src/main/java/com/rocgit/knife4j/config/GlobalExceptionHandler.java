package com.rocgit.knife4j.config;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 统一将异常转换为标准 Result 响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.ok(Result.error(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(), msg));
    }

    /** 表单绑定异常 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(), msg));
    }

    /** @RequestParam 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(), msg));
    }

    /** 主键冲突 / 唯一索引冲突（友好提示） */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDup(DuplicateKeyException e) {
        log.warn("[GlobalException] 唯一键冲突: {}", e.getMessage());
        return ResponseEntity.ok(Result.error(ResultCode.DB_CONFLICT.getCode(), "记录已存在（唯一索引冲突）"));
    }

    /** 数据完整性异常（外键/非空等） */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("[GlobalException] 数据完整性错误: {}", e.getMessage());
        return ResponseEntity.ok(Result.error(ResultCode.DB_CONSTRAINT.getCode(), "数据约束冲突：外键或非空字段不满足"));
    }

    /** JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误：JSON 解析失败"));
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissing(MissingServletRequestParameterException e) {
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(), "缺少请求参数：" + e.getParameterName()));
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(405, "请求方法 " + e.getMethod() + " 不支持"));
    }

    /** 路径不存在（需配 spring.mvc.throw-exception-if-no-handler-found=true） */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handle404(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ResultCode.NOT_FOUND.getCode(), "接口不存在：" + e.getRequestURL()));
    }

    /** 参数类型转换失败 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.ok(Result.error(ResultCode.BAD_REQUEST.getCode(),
                "参数类型错误：" + e.getName() + " 期望 " + (e.getRequiredType() == null ? "?" : e.getRequiredType().getSimpleName())));
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAll(Exception e) {
        log.error("[GlobalException] 兜底异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.FAIL.getCode(), "服务器内部错误: " + e.getClass().getSimpleName()));
    }
}
