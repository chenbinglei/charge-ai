package com.elink.evco.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 平台服务持久化装配：声明 mapper 扫描路径。
 *
 * <p>分页/乐观锁拦截器与审计时间填充由 evco-starter 统一提供； 本配置只声明服务自己的 mapper 包。
 */
@Configuration
@MapperScan("com.elink.evco.platform.iam.mapper")
public class PersistenceConfig {}
