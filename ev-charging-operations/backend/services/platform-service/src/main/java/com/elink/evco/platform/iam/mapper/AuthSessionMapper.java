package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.AuthSession;
import org.apache.ibatis.annotations.Mapper;

/** 认证会话表数据访问。 */
@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSession> {}
