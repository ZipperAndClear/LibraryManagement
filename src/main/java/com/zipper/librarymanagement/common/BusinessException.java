package com.zipper.librarymanagement.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>用于 Service 层主动抛出可预知的业务错误，如"账号不存在"、"库存不足"等。
 * 由 GlobalExceptionHandler 统一捕获并返回标准错误响应。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码（默认 510，表示业务操作失败） */
    private final Integer code;

    /**
     * 使用默认错误码（510）构造业务异常
     * @param message 错误描述信息，将直接返回给前端展示
     */
    public BusinessException(String message) {
        super(message);
        this.code = 510;
    }

    /**
     * 使用自定义错误码构造业务异常
     * @param code    自定义错误码
     * @param message 错误描述信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
