package com.zipper.librarymanagement.common;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * <p>使用 {@link RestControllerAdvice} 统一拦截 Controller 层抛出的各类异常，
 * 将其转换为 {@link Result} 标准格式 JSON 返回，避免将原始异常栈信息暴露给前端。</p>
 *
 * <h3>处理的异常类型</h3>
 * <ul>
 *   <li>{@link MaxUploadSizeExceededException} — 文件上传大小超限</li>
 *   <li>{@link DataAccessException} / {@link SQLException} — 数据库操作异常</li>
 *   <li>{@link BusinessException} — 业务逻辑异常（自定义错误码）</li>
 *   <li>{@link Exception} — 所有其他未预期的系统异常（兜底处理）</li>
 * </ul>
 *
 * @author zipper
 * @see Result
 * @see BusinessException
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理文件上传大小超限异常
     * <p>当上传文件超过 Spring 配置的 {@code spring.servlet.multipart.max-file-size}
     * 限制时触发，返回友好的提示信息及允许的最大文件大小。</p>
     *
     * @param ex 上传大小超限异常，包含最大允许字节数
     * @return 包含错误提示的 {@link Result} 对象
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> uploadSizeExceededHandler(MaxUploadSizeExceededException ex) {
        long max = ex.getMaxUploadSize();
        log.warn("上传文件超限: {} > {} bytes", max, ex.getMessage());
        return Result.error("上传文件过大，最大允许 " + (max / 1024 / 1024) + "MB");
    }

    /**
     * 处理数据库操作异常
     * <p>拦截 MyBatis/MyBatis-Plus 操作异常（{@link DataAccessException}）
     * 和原生 JDBC 异常（{@link SQLException}），记录 ERROR 级别日志
     * 并将数据库错误信息返回给前端用于问题排查。</p>
     *
     * @param ex 数据库访问异常
     * @return 包含数据库错误详情的 {@link Result} 对象
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public Result<String> dataAccessExceptionHandler(Exception ex) {
        log.error("数据库异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理业务逻辑异常
     * <p>拦截 Service 层主动抛出的 {@link BusinessException}，以 WARN 级别记录日志。
     * 将自定义业务错误码（{@code ex.getCode()}）及描述信息返回给前端，
     * 用于区分不同的业务场景（如库存不足、余额不足等）。</p>
     *
     * @param ex 业务异常，包含自定义错误码和描述
     * @return 包含业务错误码和提示信息的 {@link Result} 对象
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> businessExceptionHandler(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        if (ex.getCode() != null) {
            return Result.error(ex.getCode().toString(), ex.getMessage());
        }
        return Result.error(ex.getMessage());
    }

    /**
     * 兜底异常处理器 — 处理所有未被前面捕获的未预期异常
     * <p>拦截 {@link Exception} 及其所有子类中未被上述处理器匹配的异常，
     * 以 ERROR 级别记录完整堆栈日志，返回通用错误提示，避免敏感信息泄露。</p>
     *
     * @param ex 未预期的系统异常
     * @return 包含异常信息的 {@link Result} 对象
     */
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return Result.error(ex.getMessage());
    }
}
