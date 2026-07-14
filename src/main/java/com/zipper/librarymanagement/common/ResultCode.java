package com.zipper.librarymanagement.common;

import lombok.Getter;

/**
 * 响应码枚举，参考 HTTP 状态码的语义定义业务状态码。
 * <p>
 * 该枚举为 {@link Result} 提供标准化的状态码和提示信息，确保整个应用中的响应语义一致。
 * 目前定义了以下两种状态：
 * </p>
 * <ul>
 *   <li>{@link #SUCCESS} — 操作成功，状态码 200</li>
 *   <li>{@link #FAIL}    — 操作失败，状态码 510</li>
 * </ul>
 *
 * <p>
 * 其中 510 取自 HTTP 510 Not Extended 语义，用作应用层"业务操作失败"的默认状态码，
 * 与 HTTP 200 层级的"接口调用成功但业务处理失败"场景区分。
 * </p>
 *
 * @author Administrator
 * @see Result
 */
@Getter
public enum ResultCode {

    /**
     * 操作成功，状态码 200。
     * <p>所有正常完成的业务操作均使用此状态码响应。</p>
     */
    SUCCESS(200, "成功"),

    /**
     * 操作失败，状态码 510。
     * <p>业务层可预知的错误（如数据校验不通过、业务规则限制等）均使用此状态码响应。</p>
     */
    FAIL(510, "操作失败");

    /** 状态码数值 */
    private final int code;

    /** 状态码对应的默认提示信息 */
    private final String message;

    /**
     * 枚举构造器。
     *
     * @param code    状态码数值
     * @param message 默认提示信息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码数值。
     *
     * @return 状态码（int 类型）
     */
    public int code() {
        return code;
    }

    /**
     * 获取默认提示信息。
     *
     * @return 提示信息字符串
     */
    public String message() {
        return message;
    }

}
