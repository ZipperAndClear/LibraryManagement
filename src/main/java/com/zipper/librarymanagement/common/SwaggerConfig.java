package com.zipper.librarymanagement.common;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置类。
 * <p>
 * 基于 SpringDoc OpenAPI（兼容 OpenAPI 3.0 规范），用于自动生成 API 文档和交互式调试界面。
 * Spring 容器启动时自动注册 {@link OpenAPI} Bean，Swagger UI 可通过
 * {@code /swagger-ui.html} 或 {@code /swagger-ui/index.html} 访问。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动扫描所有 {@code @RestController} 注解的 Controller，生成 API 文档</li>
 *   <li>支持在线调试：在 Swagger UI 页面直接发送请求并查看响应</li>
 *   <li>支持导出 OpenAPI JSON 规范文件（通常位于 {@code /v3/api-docs}）</li>
 *   <li>可通过注解（{@code @Operation}、{@code @ApiResponse} 等）增强文档描述</li>
 * </ul>
 *
 * <h3>访问地址</h3>
 * <ul>
 *   <li>Swagger UI 界面：<a href="http://localhost:8080/swagger-ui/index.html">/swagger-ui/index.html</a></li>
 *   <li>OpenAPI JSON：<a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a></li>
 * </ul>
 *
 * @author zipper
 * @see OpenAPI
 * @see <a href="https://springdoc.org/">SpringDoc 官方文档</a>
 */
@Configuration
public class SwaggerConfig {

    /**
     * 注册 {@link OpenAPI} Bean，配置 API 文档的基本元信息。
     * <p>
     * 返回的 {@link OpenAPI} 对象包含：
     * </p>
     * <ul>
     *   <li><b>标题</b>：{@code librarymanagement API}</li>
     *   <li><b>版本</b>：{@code v1}</li>
     *   <li><b>许可证</b>：Apache 2.0</li>
     *   <li><b>外部文档</b>：指向项目 Wiki 或说明页面的外链</li>
     * </ul>
     *
     * <p>
     * 这些信息将显示在 Swagger UI 页面顶部，帮助 API 使用者快速了解项目概况。
     * </p>
     *
     * @return 配置了基本元信息的 OpenAPI 实例
     */
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("librarymanagement API")
                        .description("librarymanagement API 描述")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("外部文档")
                        .url("https://springshop.wiki.github.org/docs"));
    }
}
