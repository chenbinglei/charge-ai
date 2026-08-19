package com.elink.evco.platform.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 装配：注册两档权限拦截器到全部 /api/** 路由。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 两档权限拦截器。 */
    private final PermissionInterceptor permissionInterceptor;

    /**
     * 构造 MVC 配置。
     *
     * @param permissionInterceptor 权限拦截器。
     */
    public WebMvcConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    /**
     * 注册权限拦截器；仅作用于版本化 API 路由，健康检查与静态资源不拦截。
     *
     * @param registry 拦截器注册表。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
    }
}
