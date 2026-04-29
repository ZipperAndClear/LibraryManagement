package com.zipper.librarymanagement.common;

import lombok.Getter;

/**
 * @author Administrator
 * 响应码枚举，参考HTTP状态码的语义
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),//成功

    /**
     * 操作失败
     */
    FAIL(510, "操作失败");


    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

}
