package com.elink.evco.platform.iam.service.impl;

import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.mapper.IamAuditLogMapper;
import com.elink.evco.platform.iam.service.AuditService;
import com.elink.evco.web.trace.TraceIdHolder;
import org.springframework.stereotype.Service;

/** IAM 审计写入实现：组装实体并落库，traceId 取当前请求上下文。 */
@Service
public class AuditServiceImpl implements AuditService {

    /** 审计日志数据访问。 */
    private final IamAuditLogMapper auditLogMapper;

    /**
     * 构造审计服务实现。
     *
     * @param auditLogMapper 审计日志 Mapper。
     */
    public AuditServiceImpl(IamAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 追加一条审计记录；链路追踪 ID 取自当前请求上下文。
     *
     * <p>步骤：组装实体（traceId 绑定当前请求）→ 单行插入。
     *
     * @param actorId 操作者用户 ID；系统内部操作为 null。
     * @param tenantId 租户 ID；平台级操作为系统租户。
     * @param action 操作类型（IamAuditLog.ACTION_* 常量）。
     * @param targetType 目标对象类型。
     * @param targetId 目标对象 ID。
     * @param summary 脱敏操作摘要。
     * @param ip 操作来源 IP。
     */
    @Override
    public void record(
            Long actorId,
            Long tenantId,
            String action,
            String targetType,
            Long targetId,
            String summary,
            String ip) {
        // 1. 组装审计实体；traceId 绑定当前请求便于链路关联。
        IamAuditLog entry = new IamAuditLog();
        entry.setTenantId(tenantId);
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTraceId(TraceIdHolder.current().value());
        entry.setSummary(summary);
        entry.setIp(ip);
        // 2. 落库。
        auditLogMapper.insert(entry);
    }
}
