package com.zipper.librarymanagement.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置类
 * <p>实现 {@link WebMvcConfigurer} 接口，统一配置 Spring MVC 的以下组件：</p>
 * <ul>
 *   <li><b>CORS 跨域</b> — 允许所有来源的跨域请求，支持携带 Cookie</li>
 *   <li><b>静态资源映射</b> — 将上传文件目录映射为 HTTP 可访问的 {@code /uploads/**} 路径</li>
 *   <li><b>JWT 拦截器</b> — 注册 {@link JwtInterceptor} 并配置拦截/放行路径</li>
 * </ul>
 *
 * @author zipper
 * @see JwtInterceptor
 * @see WebMvcConfigurer
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * 配置 CORS 跨域访问规则
     * <p>允许所有来源（{@code allowedOriginPatterns("*")}）发起 GET、POST、PUT、
     * DELETE、OPTIONS 请求，支持携带认证信息（Cookie、Authorization 头）。
     * 预检请求缓存时间为 3600 秒。</p>
     *
     * @param registry CORS 注册器，用于添加跨域映射规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置文件上传目录的静态资源映射
     * <p>将应用配置的 {@code file.upload.path} 目录（默认为项目根目录下的 {@code uploads}）
     * 映射为 HTTP 访问路径 {@code /uploads/**}，使前端可以直接通过 URL 访问上传的文件。
     * 支持相对路径和绝对路径两种配置方式。</p>
     *
     * @param registry 资源处理器注册器，用于添加静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path p = Paths.get(uploadPath);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir"), uploadPath);
        }
        String location = "file:" + p.normalize().toAbsolutePath().toString().replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * 注册 JWT 认证拦截器并配置拦截规则
     * <p>拦截所有路径（{@code /**}），但排除以下无需认证的路径：</p>
     * <ul>
     *   <li>{@code /api/auth/login} — 登录接口</li>
     *   <li>{@code /api/auth/register} — 注册接口</li>
     *   <li>{@code /api/auth/captcha} — 验证码接口</li>
     *   <li>{@code /uploads/**} — 上传文件静态资源</li>
     *   <li>{@code /swagger-ui/**}、{@code /v3/api-docs/**}、{@code /doc.html} — API 文档</li>
     * </ul>
     *
     * @param registry 拦截器注册器，用于添加拦截器及路径模式
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/captcha",
                        "/uploads/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/doc.html"
                );
    }
}
