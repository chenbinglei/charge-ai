package com.elink.evco.platform.common.security;

import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.config.DeploymentMode;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 两档权限拦截器：@HasPermission 声明的端点校验 resource:read/write；
 * PLATFORM_SUPER 短路放行；IOT 协同模式下 iotManaged 写端点返回只读错误。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    /** 写权限码后缀。 */
    private static final String WRITE_SUFFIX = ":write";

    /** 权限读取服务。 */
    private final PermissionService permissionService;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造权限拦截器。
     *
     * @param permissionService 权限服务。
     * @param appProperties 平台业务配置。
     */
    public PermissionInterceptor(PermissionService permissionService, AppProperties appProperties) {
        this.permissionService = permissionService;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
                permissionService.effectivePermissions(
                        toUserStub(context));
        if (!permissions.contains(requiredCode)) {
            throw new BusinessException(PlatformErrorCode.FORBIDDEN, "缺少权限 " + requiredCode);
        }
        return true;
    }

    /**
     * 由认证上下文构造仅含 userType 的用户占位（避免拦截器反查用户表）。
     *
     * @param context 认证上下文。
     * @return 占位用户实体。
     */
    private IamUser toUserStub(AuthContext context) {
        IamUser stub = new IamUser();
        stub.setId(context.userId());
        stub.setUserType(context.userType());
        return stub;
    }
}
