package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.vo.RoleOptionVO;
import com.elink.evco.web.security.AuthContext;
import java.util.List;

/** 角色选项服务：仅返回当前操作者可授予的角色（操作者所属租户范围内、未删除）， 供受控下拉选择；角色写操作归 IOT/平台按部署模式维护。 */
public interface IamRoleService {

    /** 平台级角色归属的系统租户 ID（对齐 IOT linkos 默认值）。 */
    long SYSTEM_TENANT_ID = 2069700000000000001L;

    /**
     * 查询操作者可授予的角色选项；按操作者租户过滤。
     *
     * @param operator 操作者上下文。
     * @return 角色选项集合；无可授予角色时为空列表。
     */
    List<RoleOptionVO> grantableOptions(AuthContext operator);
}
