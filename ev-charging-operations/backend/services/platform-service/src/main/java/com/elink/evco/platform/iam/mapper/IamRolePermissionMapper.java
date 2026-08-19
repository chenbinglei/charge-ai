package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamRolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限直接绑定表数据访问。
 */
@Mapper
public interface IamRolePermissionMapper extends BaseMapper<IamRolePermission> {}
