package com.elink.evco.platform.iam.vo;

import java.util.List;

/**
 * 用户角色绑定替换结果。
 *
 * @param userId 用户 ID。
 * @param roles 替换后的完整角色集合。
 */
public record UserRoleBindingVO(String userId, List<RoleOptionVO> roles) {}
