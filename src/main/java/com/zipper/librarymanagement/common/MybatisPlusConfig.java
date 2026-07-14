package com.zipper.librarymanagement.common;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置类。
 * <p>
 * 注册 {@link MybatisPlusInterceptor} 拦截器，并向其中添加
 * {@link PaginationInnerInterceptor} 分页插件，使 MyBatis-Plus 的
 * {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page} 分页对象生效。
 * </p>
 *
 * <p>
 * 当前配置针对 MySQL 数据库。如果更换数据库类型，修改
 * {@code new PaginationInnerInterceptor(DbType.MYSQL)} 中的 {@link DbType} 即可。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // Mapper 接口中定义分页查询
 * Page<User> page = new Page<>(1, 10); // 第1页，每页10条
 * IPage<User> result = userMapper.selectPage(page, queryWrapper);
 * }</pre>
 *
 * @author zipper
 * @see MybatisPlusInterceptor
 * @see PaginationInnerInterceptor
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器 Bean，并装配 MySQL 分页插件。
     * <p>
     * Spring 容器启动时会自动调用此方法创建拦截器实例，
     * 分页插件会拦截 SQL 执行过程，自动在原始 SQL 后追加分页语句（如 MySQL 的 {@code LIMIT}）。
     * </p>
     *
     * @return 配置了 MySQL 分页内部拦截器的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
