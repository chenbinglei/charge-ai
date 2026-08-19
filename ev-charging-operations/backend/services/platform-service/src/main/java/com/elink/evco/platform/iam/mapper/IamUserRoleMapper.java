package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamUserRole;
import org.apache.ibatis.annotations.Mapper;

/** 用户-角色绑定表数据访问。 */
@Mapper
public interface IamUserRoleMapper extends BaseMapper<IamUserRole> {}
