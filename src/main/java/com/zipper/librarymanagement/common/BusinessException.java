package com.zipper.librarymanagement.common;

import lombok.Getter;

/**
 * 自定义业务异常，用于 Service 层主动抛出可预知的业务错误。
 * <p>
 * 当业务逻辑不满足预期条件时（如"账号不存在"、"密码错误"、"库存不足"、"权限不足"等），
 * Service 层应抛出此异常，由全局异常处理器 {@code GlobalExceptionHandler}
 * 统一捕获并转换为标准 {@link Result} 错误响应返回给前端。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用默认错误码 510
 * throw new BusinessException("账号或密码错误");
 *
 * // 使用自定义错误码
 * throw new BusinessException(401, "用户未登录，请先登录");
 * throw new BusinessException(403, "权限不足，无法执行此操作");
 * }</pre>
 *
 * <p>
 * 继承自 {@link RuntimeException}，因此无需在方法签名中显式声明 {@code throws}，
 * Spring 事务管理器也会在遇到此异常时自动回滚事务。
 * </p>
 *
 * @author zipper
 * @see Result
 * @see ResultCode
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，默认 510（对应 {@link ResultCode#FAIL}），可自定义为其他值以区分不同错误类型 */
    private final Integer code;

    /**
     * 使用默认错误码 510 构造业务异常。
     *
     * @param message 错误描述信息，将直接返回给前端展示
     */
    public BusinessException(String message) {
        super(message);
        this.code = 510;
    }

    /**
     * 使用自定义错误码构造业务异常。
     * <p>适用于需要根据错误码做前端差异化处理的场景，如 401 未登录、403 无权限等。</p>
     *
     * @param code    自定义业务错误码
     * @param message 错误描述信息，将直接返回给前端展示
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
