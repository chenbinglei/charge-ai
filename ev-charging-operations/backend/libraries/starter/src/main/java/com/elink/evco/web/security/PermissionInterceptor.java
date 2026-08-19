package com.elink.evco.web.security;

import com.elink.evco.web.config.AppProperties;
import com.elink.evco.web.config.DeploymentMode;
import com.elink.evco.web.error.BusinessException;
import com.elink.evco.web.error.PlatformErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 两档权限拦截器：@HasPermission 声明的端点校验 resource:read/write； PLATFORM_SUPER 短路放行；IOT 协同模式下 iotManaged
 * 写端点返回只读错误。
 */
public class PermissionInterceptor implements HandlerInterceptor {

    /** 写权限码后缀。 */
    private static final String WRITE_SUFFIX = ":write";

    /** 权限读取端口；由宿主服务实现。 */
    private final PermissionCheckPort permissionCheck;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造权限拦截器。
     *
     * @param permissionCheck 权限读取端口。
     * @param appProperties 平台业务配置。
     */
    public PermissionInterceptor(PermissionCheckPort permissionCheck, AppProperties appProperties) {
        this.permissionCheck = permissionCheck;
        this.appProperties = appProperties;
    }

    /**
     * 前置权限校验；未认证、越权与只读域写操作分别返回登记错误码。
     *
     * @param request 当前请求。
     * @param response 响应。
     * @param handler 目标处理器。
     * @return true 放行。
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        HasPermission annotation =
                handlerMethod.hasMethodAnnotation(HasPermission.class)
                        ? handlerMethod.getMethodAnnotation(HasPermission.class)
                        : handlerMethod.getBeanType().getAnnotation(HasPermission.class);
        if (annotation == null) {
            return true;
        }
        AuthContext context = AuthContextHolder.current();
        if (context == null) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED);
        }
        String requiredCode = annotation.value();
        if (annotation.iotManaged()
                && requiredCode.endsWith(WRITE_SUFFIX)
                && appProperties.getDeploymentMode() == DeploymentMode.IOT_COLLABORATIVE) {
            throw new BusinessException(PlatformErrorCode.DEPLOYMENT_MODE_READONLY);
        }
        if (context.isPlatformSuperAdmin()) {
            return true;
        }
        Set<String> permissions =
                permissionCheck.effectivePermissions(context.userId(), context.userType());
        if (!permissions.contains(requiredCode)) {
            throw new BusinessException(PlatformErrorCode.FORBIDDEN, "缺少权限 " + requiredCode);
        }
        return true;
    }
}
