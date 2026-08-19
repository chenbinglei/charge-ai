package com.elink.evco.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elink.evco.platform.common.security.AuthContext;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.vo.RoleOptionVO;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 角色选项服务：仅返回当前操作者可授予的角色（操作者所属租户范围内、未删除），
 * 供受控下拉选择；角色写操作归 IOT/平台按部署模式维护。
 */
@Service
public class IamRoleService {

    /** 平台级角色归属的系统租户 ID（对齐 IOT linkos 默认值）。 */
    public static final long SYSTEM_TENANT_ID = 2069700000000000001L;

    /** 角色数据访问。 */
    private final IamRoleMapper roleMapper;

    /**
     * 构造角色选项服务。
     *
     * @param roleMapper 角色 Mapper。
     */
    public IamRoleService(IamRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 查询操作者可授予的角色选项；按操作者租户过滤。
     *
     * @param operator 操作者上下文。
     * @return 角色选项集合；无可授予角色时为空列表。
     */
    public List<RoleOptionVO> grantableOptions(AuthContext operator) {
        List<IamRole> roles =
                roleMapper.selectList(
                        new LambdaQueryWrapper<IamRole>()
                                .eq(IamRole::getTenantId, operator.tenantId())
                                .isNull(IamRole::getDeletedAt)
                                .orderByAsc(IamRole::getId));
        return roles.stream().map(this::toOption).toList();
    }

    /**
     * 转换角色选项；scope 按租户归属推导（系统租户=platform，其余=tenant）。
     *
     * @param role 角色实体。
     * @return 角色选项 VO。
     */
    private RoleOptionVO toOption(IamRole role) {
        String scope = role.getTenantId() != null && role.getTenantId() == SYSTEM_TENANT_ID ? "platform" : "tenant";
        return new RoleOptionVO(
                String.valueOf(role.getId()), role.getName(), scope, role.getDescription());
    }
}
