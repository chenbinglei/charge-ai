package com.elink.evco.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级两档权限声明：GET 接口校验 resource:read，写接口校验 resource:write。
 *
 * <p>权限不足返回 FORBIDDEN(403)；PLATFORM_SUPER 用户短路放行。 iotManaged=true 的写接口在 IOT 协同模式下返回
 * DEPLOYMENT_MODE_READONLY(403)。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {

    /** 要求的权限码，格式 resource:read / resource:write。 */
    String value();

    /**
     * 是否属于 IOT 维护数据域：IOT 协同模式下该域写接口被拦截返回只读错误。
     *
     * @return true 表示 IOT 协同模式下禁写。
     */
    boolean iotManaged() default false;
}
