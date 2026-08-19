package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 审计日志表数据访问；追加只写。
 */
@Mapper
public interface IamAuditLogMapper extends BaseMapper<IamAuditLog> {}
