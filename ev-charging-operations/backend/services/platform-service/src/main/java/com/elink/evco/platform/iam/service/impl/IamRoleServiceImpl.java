package com.elink.evco.platform.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.service.IamRoleService;
import com.elink.evco.platform.iam.vo.RoleOptionVO;
import com.elink.evco.web.security.AuthContext;
import java.util.List;
import org.springframework.stereotype.Service;

/** 角色选项实现：租户范围内查询未删除角色并转换为受控选项。 */
@Service
public class IamRoleServiceImpl implements IamRoleService {

    /** 角色数据访问。 */
    private final IamRoleMapper roleMapper;

    /**
     * 构造角色选项实现。
     *
     * @param roleMapper 角色 Mapper。
     */
    public IamRoleServiceImpl(IamRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 查询操作者可授予的角色选项；按操作者租户过滤。
     *
     * <p>步骤：按租户查询未删除角色 → 逐个转换为下拉选项。
     *
     * @param operator 操作者上下文。
     * @return 角色选项集合；无可授予角色时为空列表。
     */
    @Override
    public List<RoleOptionVO> grantableOptions(AuthContext operator) {
        // 1. 只查操作者租户范围内、未删除的角色。
        List<IamRole> roles =
                roleMapper.selectList(
                        new LambdaQueryWrapper<IamRole>()
                                .eq(IamRole::getTenantId, operator.tenantId())
                                .isNull(IamRole::getDeletedAt)
                                .orderByAsc(IamRole::getId));
        // 2. 转换为受控下拉选项。
        return roles.stream().map(this::toOption).toList();
    }

    /**
     * 转换角色选项；scope 按租户归属推导（系统租户=platform，其余=tenant）。
     *
     * @param role 角色实体。
     * @return 角色选项 VO。
     */
    private RoleOptionVO toOption(IamRole role) {
        String scope =
                role.getTenantId() != null && role.getTenantId() == SYSTEM_TENANT_ID
                        ? "platform"
                        : "tenant";
        return new RoleOptionVO(
                String.valueOf(role.getId()), role.getName(), scope, role.getDescription());
    }
}
