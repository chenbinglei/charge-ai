package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** IAM 操作审计日志（iam_audit_log）：追加只写，不更新不删除；90 天后冷存储归档。 */
@Setter
@Getter
@TableName("iam_audit_log")
public class IamAuditLog {

    /** 操作：登录。 */
    public static final String ACTION_LOGIN = "login";

    /** 操作：登出。 */
    public static final String ACTION_LOGOUT = "logout";

    /** 操作：令牌撤销（强制下线）。 */
    public static final String ACTION_TOKEN_REVOKE = "token_revoke";

    /** 操作：创建。 */
    public static final String ACTION_CREATE = "create";

    /** 操作：更新。 */
    public static final String ACTION_UPDATE = "update";

    /** 操作：逻辑删除。 */
    public static final String ACTION_DELETE = "delete";

    /** 操作：状态迁移。 */
    public static final String ACTION_STATUS_CHANGE = "status_change";

    /** 操作：角色绑定替换。 */
    public static final String ACTION_ROLE_REPLACE = "role_replace";

    /** 日志 ID（雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID；平台级操作为系统租户。 */
    private Long tenantId;

    /** 操作者用户 ID；系统内部操作为 NULL。 */
    private Long actorId;

    /** 操作类型：create/update/delete/status_change/role_replace/login/logout/token_revoke。 */
    private String action;

    /** 目标对象类型：iam_user/iam_role/auth_session 等。 */
    private String targetType;

    /** 目标对象 ID。 */
    private Long targetId;

    /** 请求链路追踪 ID。 */
    private String traceId;

    /** 脱敏操作摘要；不含密码哈希、令牌原文等敏感材料。 */
    private String summary;

    /** 操作来源 IP；登录/SSO 审计必填。 */
    private String ip;

    /** 操作时间（UTC），插入自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
