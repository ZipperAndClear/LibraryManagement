package com.zipper.librarymanagement.common;

import java.sql.SQLException;
import com.zipper.librarymanagement.common.Result;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice

public class GlobalExceptionHandler {
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public Result<String> dataAccessExceptionHandler(Exception ex){
        log.error("数据库异常", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception ex){
        log.error("系统异常", ex.getMessage());
        return Result.error(ex.getMessage());
    }

}
