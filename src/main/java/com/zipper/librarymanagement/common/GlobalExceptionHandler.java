package com.zipper.librarymanagement.common;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * <p>使用 @RestControllerAdvice 统一拦截 Controller 层抛出的各类异常，
 * 将其转换为 Result 标准格式返回，避免将原始异常栈信息暴露给前端。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理数据库操作异常
     * <p>拦截 DataAccessException（MyBatis 操作异常）和 SQLException</p>
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public Result<String> dataAccessExceptionHandler(Exception ex) {
        log.error("数据库异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理业务异常
     * <p>拦截 Service 层主动抛出的 BusinessException，以 WARN 级别记录日志，
     * 并将自定义错误码返回给前端。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> businessExceptionHandler(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ex.getCode().toString(), ex.getMessage());
    }

    /**
     * 处理未预期的系统异常（兜底处理器）
     */
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return Result.error(ex.getMessage());
    }
}
