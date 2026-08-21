package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 认证会话（auth_session）：密码登录与 SSO 免登录共用；短生命周期运行数据， 无逻辑删除，生命周期由 status 表达。 */
@Setter
@Getter
@TableName("auth_session")
public class AuthSession {

    /** 状态：有效。 */
    public static final String STATUS_ACTIVE = "active";

    /** 状态：TTL 过期。 */
    public static final String STATUS_EXPIRED = "expired";

    /** 状态：登出或撤销。 */
    public static final String STATUS_REVOKED = "revoked";

    /** 认证方式：账号密码登录。 */
    public static final String AUTH_TYPE_PASSWORD = "PASSWORD";

    /** 认证方式：IOT 协同模式免登录换会话。 */
    public static final String AUTH_TYPE_SSO = "SSO";

    /** 会话 ID（雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID（iam_user）。 */
    private Long userId;

    /** 认证方式：PASSWORD/SSO。 */
    private String authType;

    /** 状态：active/expired/revoked。 */
    private String status;

    /** 登录来源 IP（IPv4；纯 IPv6 来源存 NULL）。 */
    private String ip;

    /** 登录端 User-Agent 摘要。 */
    private String userAgent;

    /** 会话过期时间（北京时间）；随 refresh 滑动续期。 */
    private LocalDateTime expiresAt;

    /** 创建时间（北京时间），插入自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（北京时间），插入/更新自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
