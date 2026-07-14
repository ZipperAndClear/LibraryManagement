package com.zipper.librarymanagement.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 令牌验证拦截器
 * <p>实现 {@link HandlerInterceptor}，在请求到达 Controller 之前对
 * 所有 HTTP 请求进行 JWT 身份验证。从 {@code Authorization} 请求头中
 * 提取 Bearer Token，调用 {@link JwtUtil#parseJWT} 验证签名和有效期。</p>
 *
 * <h3>验证流程</h3>
 * <ol>
 *   <li>放行 OPTIONS 预检请求（CORS 跨域需要）</li>
 *   <li>检查 Authorization 请求头是否存在且以 {@code Bearer } 开头</li>
 *   <li>解析 JWT 并验证签名、有效期</li>
 *   <li>验证通过后将 {@code userId} 和 {@code username} 存入 request 属性</li>
 *   <li>验证失败返回 HTTP 401 及 JSON 格式错误信息</li>
 * </ol>
 *
 * @author zipper
 * @see JwtUtil
 * @see HandlerInterceptor
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.secretKey}")
    private String secretKey;

    /**
     * 请求前置处理 — 验证 JWT 令牌
     * <p>从请求头中提取并验证 JWT，解析通过后将用户信息写入 request 属性。
     * 放行 OPTIONS 预检请求，拒绝未登录或 Token 无效的请求。</p>
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应，用于写入 401 状态码
     * @param handler  目标处理器
     * @return {@code true} 放行请求，{@code false} 拦截请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求（CORS 需要）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        if (!authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Token格式错误");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "Token不能为空");
            return false;
        }

        try {
            Jws<Claims> claimsJws = JwtUtil.parseJWT(secretKey, token);
            Claims claims = claimsJws.getPayload();
            log.info("Token解析成功，用户ID: {}, 用户名: {}", claims.get("userId"), claims.get("username"));
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("username", claims.get("username"));
            request.setAttribute("roles", claims.get("roles"));
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期", e);
            writeUnauthorized(response, "Token已过期，请重新登录");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token无效", e);
            writeUnauthorized(response, "Token无效，请重新登录");
            return false;
        }
    }

    /**
     * 向客户端写入 HTTP 401 未授权 JSON 响应
     * <p>设置响应状态码为 401，Content-Type 为 {@code application/json;charset=UTF-8}，
     * 并将错误信息以 JSON 格式写入响应体。</p>
     *
     * @param response HTTP 响应对象
     * @param message  返回给前端的错误提示信息
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
