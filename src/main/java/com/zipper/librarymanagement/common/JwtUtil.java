package com.zipper.librarymanagement.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT（JSON Web Token）令牌工具类。
 * <p>
 * 使用 HMAC-SHA256（HS256）算法对用户身份信息进行签名，生成防篡改的认证令牌。
 * 客户端在请求头中携带该令牌，服务端通过共享密钥验签以确认用户身份。
 * </p>
 *
 * <h3>核心流程</h3>
 * <ol>
 *   <li>用户登录成功后，调用 {@link #createJWT(String, long, Map)} 生成 token 返回给客户端</li>
 *   <li>客户端后续请求携带 token（通常放在 {@code Authorization} 头中）</li>
 *   <li>服务端拦截器/过滤器调用 {@link #parseJWT(String, String)} 解析并验证 token</li>
 * </ol>
 *
 * <h3>安全注意事项</h3>
 * <ul>
 *   <li>密钥（secretKey）至少需要 32 字符（256 位），以满足 HS256 算法要求</li>
 *   <li>密钥应与 {@code application-dev.yaml} 中的配置一致，且不应硬编码在代码中</li>
 *   <li>token 的过期时间（ttlMillis）应合理设置，避免过长导致安全风险或过短影响用户体验</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 生成 token
 * Map<String, Object> claims = new HashMap<>();
 * claims.put("userId", user.getId());
 * claims.put("username", user.getUsername());
 * String token = JwtUtil.createJWT(secretKey, 3600000, claims); // 1小时有效
 *
 * // 解析 token
 * Jws<Claims> jws = JwtUtil.parseJWT(secretKey, token);
 * Integer userId = jws.getPayload().get("userId", Integer.class);
 * }</pre>
 *
 * @author zipper
 * @see <a href="https://github.com/jwtk/jjwt">JJWT 官方文档</a>
 */
public class JwtUtil {

    /**
     * 生成签名的 JWT 令牌。
     * <p>
     * 使用 HS256 算法对自定义 claims 进行签名，并设置过期时间。
     * 生成的 token 格式为 {@code header.payload.signature}，可安全地在客户端与服务端之间传输。
     * </p>
     *
     * @param secretKey 签名密钥，至少 32 字符（256 位），需与 {@code application-dev.yaml} 中配置一致
     * @param ttlMillis token 有效期，单位毫秒（例如 3600000 表示 1 小时）
     * @param claims    自定义负载信息，通常包含 {@code userId}、{@code username} 等用户标识
     * @return 签名的 JWT 字符串，可直接返回给客户端
     * @throws io.jsonwebtoken.security.WeakKeyException 如果密钥长度不足
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecureDigestAlgorithm<SecretKey, SecretKey> algorithm = Jwts.SIG.HS256;
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.builder()
                .signWith(key, algorithm)
                .expiration(exp)
                .claims(claims)
                .compact();
    }

    /**
     * 解析并验证 JWT 令牌。
     * <p>
     * 使用共享密钥对 token 进行验签，如果 token 被篡改、已过期或签名不匹配，
     * 将抛出相应的异常（如 {@code ExpiredJwtException}、{@code SignatureException} 等）。
     * </p>
     *
     * @param secretKey 签名密钥，必须与生成 token 时使用的密钥完全一致
     * @param token     客户端传入的 JWT 字符串
     * @return 解析后的 {@link Jws} 对象，可通过 {@link Jws#getPayload()} 获取 {@link Claims}，
     *         进而提取 {@code userId}、{@code username} 等自定义负载
     * @throws io.jsonwebtoken.ExpiredJwtException      如果 token 已过期
     * @throws io.jsonwebtoken.security.SignatureException 如果签名验证失败（token 被篡改或密钥不匹配）
     * @throws io.jsonwebtoken.MalformedJwtException    如果 token 格式不正确
     */
    public static Jws<Claims> parseJWT(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
