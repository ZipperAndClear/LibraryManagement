package com.zipper.librarymanagement.dto;

import lombok.Data;

/**
 * 学生注册数据传输对象
 * <p>前端注册表单提交的数据结构，比 Entity 多了确认密码和验证码字段</p>
 */
@Data
public class RegisterDTO {

    /** 登录账号（学号） */
    private String username;

    /** 密码（明文，后端 BCrypt 加密后存库） */
    private String password;

    /** 确认密码（用于前端校验两次密码一致，后端也需要校验一次） */
    private String confirmPassword;

    /** 真实姓名 */
    private String realName;

    /** 图形验证码用户输入值 */
    private String captcha;

    /** 验证码缓存键（与 CaptchaVO.captchaKey 对应） */
    private String captchaKey;
}
