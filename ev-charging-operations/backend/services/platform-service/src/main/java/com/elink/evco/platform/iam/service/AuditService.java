package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.common.web.TraceIdHolder;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.mapper.IamAuditLogMapper;
import org.springframework.stereotype.Service;

/**
 * IAM 审计写入：敏感操作（用户 CRUD、角色变更、登录/登出/撤销）统一落 iam_audit_log。
 *
 * <p>摘要必须脱敏：不含密码哈希、令牌原文、SSO ticket 原文等敏感材料。
 */
@Service
public class AuditService {

    /** 审计日志数据访问。 */
    private final IamAuditLogMapper auditLogMapper;

    /**
     * 构造审计服务。
     *
     * @param auditLogMapper 审计日志 Mapper。
     */
    public AuditService(IamAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 追加一条审计记录；链路追踪 ID 取自当前请求上下文。
     *
     * @param actorId 操作者用户 ID；系统内部操作为 null。
     * @param tenantId 租户 ID；平台级操作为系统租户。
     * @param action 操作类型（IamAuditLog.ACTION_* 常量）。
     * @param targetType 目标对象类型。
     * @param targetId 目标对象 ID。
     * @param summary 脱敏操作摘要。
     * @param ip 操作来源 IP。
     */
    public void record(
            Long actorId,
            Long tenantId,
            String action,
            String targetType,
            Long targetId,
            String summary,
            String ip) {
        IamAuditLog entry = new IamAuditLog();
        entry.setTenantId(tenantId);
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTraceId(TraceIdHolder.current().value());
        entry.setSummary(summary);
        entry.setIp(ip);
        auditLogMapper.insert(entry);
    }
}
