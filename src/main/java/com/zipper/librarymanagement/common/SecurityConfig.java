package com.zipper.librarymanagement.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关配置类。
 * <p>
 * 提供 {@link BCryptPasswordEncoder} 单例 Bean，用于用户密码的加密存储和登录校验。
 * BCrypt 是一种自适应哈希算法，内置盐值（salt）和可调节的计算强度，
 * 能有效抵御彩虹表攻击和暴力破解。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>注册时</b>：对用户输入的明文密码进行加密后存入数据库</li>
 *   <li><b>登录时</b>：将用户输入的明文密码与数据库中存储的密文进行比对校验</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 注册：加密密码
 * String encodedPassword = passwordEncoder.encode(rawPassword);
 * user.setPassword(encodedPassword);
 *
 * // 登录：校验密码
 * boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
 * }</pre>
 *
 * 注意：此类仅负责提供密码编码器 Bean，不包含 Spring Security 的过滤器链配置。
 *
 * @author zipper
 * @see BCryptPasswordEncoder
 */
@Configuration
public class SecurityConfig {

    /**
     * 注册 {@link BCryptPasswordEncoder} 单例 Bean。
     * <p>
     * 使用默认强度因子（10），该值在安全性和性能之间取得平衡：
     * 强度每增加 1，哈希计算时间翻倍。10 是目前生产环境的推荐值。
     * </p>
     *
     * @return BCryptPasswordEncoder 实例，供整个应用注入使用
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
