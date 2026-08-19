package com.elink.evco.platform.iam.service;

/**
 * IAM 审计写入：敏感操作（用户 CRUD、角色变更、登录/登出/撤销）统一落 iam_audit_log。
 *
 * <p>摘要必须脱敏：不含密码哈希、令牌原文、SSO ticket 原文等敏感材料。
 */
public interface AuditService {

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
    void record(
            Long actorId,
            Long tenantId,
            String action,
            String targetType,
            Long targetId,
            String summary,
            String ip);
}
