package com.elink.evco.platform.iam.controller;

import com.elink.evco.kernel.api.ApiResponse;
import com.elink.evco.platform.iam.service.IamRoleService;
import com.elink.evco.platform.iam.vo.RoleOptionVO;
import com.elink.evco.web.security.AuthContextHolder;
import com.elink.evco.web.security.HasPermission;
import com.elink.evco.web.trace.TraceIdHolder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IAM 角色端点：角色为 IOT 维护数据域，本端点仅提供可授予角色选项（纯查看）； 角色写操作不在平台侧提供（由 IOT 推送或独立部署模式下后续模块维护）。 */
@RestController
@RequestMapping("/api/v1/iam/roles")
public class IamRoleController {

    /** 角色选项服务。 */
    private final IamRoleService roleService;

    /**
     * 构造角色端点。
     *
     * @param roleService 角色选项服务。
     */
    public IamRoleController(IamRoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 查询当前操作者可授予的角色选项；供受控下拉选择。
     *
     * @return 可授予角色集合。
     */
    @GetMapping("/options")
    @HasPermission("iam_role:read")
    public ApiResponse<List<RoleOptionVO>> roleOptions() {
        List<RoleOptionVO> data = roleService.grantableOptions(AuthContextHolder.require());
        return ApiResponse.success(data, TraceIdHolder.current().value());
    }
}
