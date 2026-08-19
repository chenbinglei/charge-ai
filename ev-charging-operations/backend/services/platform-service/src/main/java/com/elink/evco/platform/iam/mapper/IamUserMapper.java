package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 用户表数据访问；查询条件由服务层以 Wrapper 显式组装（含软删过滤）。
 */
@Mapper
public interface IamUserMapper extends BaseMapper<IamUser> {}
