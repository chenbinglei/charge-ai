package com.elink.evco.web.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 安全配置（application.yml 的 evco.web.security 前缀）。
 *
 * <p>免认证端点由宿主服务声明，格式 "METHOD /path"；格式非法在过滤器构造时快速失败， 防止白名单配置错误导致鉴权静默放开。
 */
@Setter
@ConfigurationProperties(prefix = "evco.web.security")
public class WebSecurityProperties {

    /** 管理端鉴权总开关；个人小程序等无权限服务置 false 时 Bearer 过滤器与权限拦截器均不装配。 */
    private boolean enabled = true;

    /** 免认证精确端点集合，格式 "METHOD /path"。 */
    private List<String> anonymousEndpoints = new ArrayList<>();

    /** 免认证路径前缀集合；健康检查与错误转发默认放行。 */
    private List<String> anonymousPrefixes = new ArrayList<>(List.of("/actuator", "/error"));

    /**
     * @return 管理端鉴权是否启用。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return 免认证精确端点集合。
     */
    public List<String> getAnonymousEndpoints() {
        return anonymousEndpoints;
    }

    /**
     * @return 免认证路径前缀集合。
     */
    public List<String> getAnonymousPrefixes() {
        return anonymousPrefixes;
    }
}
