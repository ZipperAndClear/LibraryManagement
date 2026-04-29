package com.zipper.librarymanagement.common;

import lombok.Data;

/**
 * 统一 API 响应体
 * <p>所有 Controller 接口都通过此类封装返回结果，确保前端接收数据格式统一。</p>
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {

    /** 操作代码：200-成功 510-失败 */
    Integer code;

    /** 提示信息 */
    String message;

    /** 结果数据（成功时携带，失败时为 null） */
    T data;

    public Result(ResultCode resultCode) {
        this.code = resultCode.code();
        this.message = resultCode.message();
    }

    public Result(ResultCode resultCode, T data) {
        this.code = resultCode.code();
        this.message = resultCode.message();
        this.data = data;
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 成功返回（无数据） */
    public static Result<String> success() {
        return new Result<String>(ResultCode.SUCCESS);
    }

    /** 成功返回（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS, data);
    }

    /** 失败返回（使用默认失败提示信息） */
    public static Result<String> error() {
        return new Result<String>(ResultCode.FAIL);
    }

    /** 失败返回（使用指定枚举中的状态码和提示信息） */
    public static Result<String> error(ResultCode resultCode) {
        return new Result<String>(resultCode);
    }

    /** 失败返回（使用默认失败状态码，自定义提示信息） */
    public static Result<String> error(String message) {
        return new Result<String>(ResultCode.FAIL, message);
    }

    /** 失败返回（使用指定枚举状态码，自定义提示信息） */
    public static Result<String> error(ResultCode resultCode, String message) {
        return new Result<String>(resultCode, message);
    }

    /** 失败返回（自定义状态码和提示信息） */
    public static Result<String> error(String code, String message) {
        return new Result<String>(Integer.valueOf(code), message);
    }
}
