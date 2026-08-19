package com.elink.evco.platform.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全组件装配：密码哈希器统一使用 BCrypt（内置盐值）。
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * 提供 BCrypt 密码编码器；强度默认 10 轮，哈希结果含盐自校验。
     *
     * @return 密码编码器。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
