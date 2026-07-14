package com.zipper.librarymanagement.common;

import lombok.Data;

/**
 * 统一 API 响应体，所有 Controller 接口均通过此类封装返回结果，确保前端接收数据格式统一。
 * <p>
 * 该类是前端与后端交互的核心数据结构，无论业务操作成功或失败，都以统一的 JSON 格式响应。
 * 成功时 {@code code} 为 200、{@code data} 携带业务数据；失败时 {@code code} 为 510（或自定义），
 * {@code message} 携带错误描述，{@code data} 为 {@code null}。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 成功返回（无数据）
 * return Result.success();
 *
 * // 成功返回（带数据）
 * return Result.success(user);
 *
 * // 失败返回（默认提示）
 * return Result.error();
 *
 * // 失败返回（自定义提示信息）
 * return Result.error("用户名或密码错误");
 *
 * // 失败返回（使用指定 ResultCode 枚举）
 * return Result.error(ResultCode.FAIL);
 * }</pre>
 *
 * @param <T> 响应数据的类型，成功时携带具体业务对象，失败时为 {@code null}
 * @author zipper
 * @see ResultCode
 */
@Data
public class Result<T> {

    /** 操作状态码，200 表示成功，510 表示业务失败 */
    Integer code;

    /** 提示信息，成功时为 "成功"，失败时携带具体错误原因 */
    String message;

    /** 响应数据，成功时携带业务对象，失败时为 {@code null} */
    T data;

    /**
     * 使用 {@link ResultCode} 枚举构造响应体（不含数据）。
     *
     * @param resultCode 状态码枚举，提供 {@code code} 和 {@code message}
     */
    public Result(ResultCode resultCode) {
        this.code = resultCode.code();
        this.message = resultCode.message();
    }

    /**
     * 使用 {@link ResultCode} 枚举构造响应体，并携带业务数据。
     *
     * @param resultCode 状态码枚举，提供 {@code code} 和 {@code message}
     * @param data       业务数据
     */
    public Result(ResultCode resultCode, T data) {
        this.code = resultCode.code();
        this.message = resultCode.message();
        this.data = data;
    }

    /**
     * 使用自定义状态码和提示信息构造响应体。
     *
     * @param code    自定义状态码
     * @param message 提示信息
     */
    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 成功返回（无数据），状态码固定为 200，提示信息为 "成功"。
     *
     * @param <T> 响应数据类型（由编译器推断）
     * @return 成功响应体，{@code data} 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success() {
        return (Result<T>) new Result(ResultCode.SUCCESS);
    }

    /**
     * 成功返回（带数据），状态码固定为 200，提示信息为 "成功"。
     *
     * @param data 业务数据
     * @param <T>  响应数据类型
     * @return 成功响应体，{@code data} 携带传入的业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS, data);
    }

    /**
     * 失败返回（默认提示），状态码为 510，提示信息为 "操作失败"。
     *
     * @param <T> 响应数据类型
     * @return 失败响应体，{@code data} 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error() {
        return (Result<T>) new Result(ResultCode.FAIL);
    }

    /**
     * 失败返回（使用指定 {@link ResultCode} 枚举的状态码和提示信息）。
     *
     * @param resultCode 状态码枚举
     * @param <T>        响应数据类型
     * @return 失败响应体，{@code data} 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(ResultCode resultCode) {
        return (Result<T>) new Result(resultCode);
    }

    /**
     * 失败返回（使用默认失败状态码 510，自定义提示信息）。
     *
     * @param message 自定义错误提示信息
     * @param <T>     响应数据类型
     * @return 失败响应体，{@code code} 为 510，{@code data} 为 {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Result<T> error(String message) {
        return new Result(ResultCode.FAIL, message);
    }

    /**
     * 失败返回（使用指定 {@link ResultCode} 枚举状态码，自定义提示信息）。
     * <p>适用于需要复用已有枚举的状态码，但想覆盖默认提示信息的场景。</p>
     *
     * @param resultCode 状态码枚举（仅提取 {@code code} 字段）
     * @param message    自定义错误提示信息
     * @param <T>        响应数据类型
     * @return 失败响应体，{@code data} 为 {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result(resultCode, message);
    }

    /**
     * 失败返回（自定义状态码和提示信息，完全不受枚举约束）。
     *
     * @param code    自定义状态码字符串（内部转为 {@code Integer}）
     * @param message 自定义错误提示信息
     * @param <T>     响应数据类型
     * @return 失败响应体，{@code data} 为 {@code null}
     * @throws NumberFormatException 如果 {@code code} 不是有效的整数字符串
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Result<T> error(String code, String message) {
        return new Result(Integer.valueOf(code), message);
    }
}
