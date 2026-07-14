package com.zipper.librarymanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 图书管理系统启动类
 * <p>Spring Boot 应用程序入口，使用 {@link MapperScan} 注解自动扫描
 * {@code com.zipper.librarymanagement.mapper} 包下的所有 MyBatis 映射接口，
 * 并将其注册为 Spring Bean。{@link SpringBootApplication} 启用自动配置、
 * 组件扫描等 Spring Boot 核心能力。</p>
 *
 * <h3>启动方式</h3>
 * <ul>
 *   <li>IDE 中直接运行 {@code main} 方法</li>
 *   <li>命令行执行 {@code mvn spring-boot:run}</li>
 *   <li>打包后执行 {@code java -jar LibraryManagement.jar}</li>
 * </ul>
 *
 * @author zipper
 * @see SpringBootApplication
 * @see MapperScan
 */
@MapperScan("com.zipper.librarymanagement.mapper")
@SpringBootApplication
public class LibraryManagementApplication {

    /**
     * 应用程序主入口方法
     * <p>调用 {@link SpringApplication#run} 启动嵌入式的 Tomcat 服务器，
     * 加载 Spring 应用上下文，完成所有 Bean 的初始化和依赖注入。</p>
     *
     * @param args 命令行参数，可传递给 Spring Boot 应用进行外部化配置
     */
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
