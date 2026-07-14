package com.zipper.librarymanagement.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码响应值对象
 * <p>用于传输验证码图片和对应的缓存键，前端需在后续请求中携带 captchaKey + 用户输入值</p>
 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码缓存键（UUID），用于在 Redis/本地缓存中查找对应的答案 */
    private String captchaKey;

    /** 验证码图片（Base64 编码的 SVG 图片，含 data:image/svg+xml;base64, 前缀） */
    private String captchaImage;
}
