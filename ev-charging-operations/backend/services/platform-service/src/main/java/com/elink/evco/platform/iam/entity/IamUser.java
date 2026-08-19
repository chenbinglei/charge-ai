package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * IAM 管理端用户（iam_user）：状态机 active/locked/disabled，软删释放登录名。
 *
 * <p>IOT 协同模式下由 IOT 推送维护（source=IOT_PUSH、沿用 IOT 用户 ID）；
 * 平台扩展字段（locked_until 等）推送时不被覆盖（设计包 §8.3）。
 */
@Setter
@Getter
@TableName("iam_user")
public class IamUser {

    /** 状态：可用。 */
    public static final String STATUS_ACTIVE = "active";

    /** 状态：锁定（连续失败或到期自动锁定）。 */
    public static final String STATUS_LOCKED = "locked";

    /** 状态：停用。 */
    public static final String STATUS_DISABLED = "disabled";

    /** 用户类型：平台超级管理员，鉴权短路放行。 */
    public static final String TYPE_PLATFORM_SUPER = "PLATFORM_SUPER";

    /** 用户类型：租户管理员。 */
    public static final String TYPE_TENANT_ADMIN = "TENANT_ADMIN";

    /** 用户类型：普通运营人员。 */
    public static final String TYPE_NORMAL = "NORMAL";

    /** 数据来源：平台维护。 */
    public static final String SOURCE_PLATFORM = "PLATFORM";

    /** 数据来源：IOT 协同模式推送。 */
    public static final String SOURCE_IOT_PUSH = "IOT_PUSH";

    /** 用户 ID（雪花 ID；IOT 模式沿用 IOT 推送 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户；平台级用户归属系统租户。 */
    private Long tenantId;

    /** 登录名；租户范围内唯一（生成列 active_username 实现软删后释放，不在实体映射）。 */
    private String username;

    /** 后台展示姓名。 */
    private String displayName;

    /** BCrypt 密码哈希（含内置盐值）。 */
    private String passwordHash;

    /** 用户类型：PLATFORM_SUPER/TENANT_ADMIN/NORMAL。 */
    private String userType;

    /** IOT 平台用户 ID；SSO 免登录定位键，source=IOT_PUSH 时必填。 */
    private Long iotUserId;

    /** 数据来源：PLATFORM/IOT_PUSH。 */
    private String source;

    /** 状态：active/locked/disabled。 */
    private String status;

    /** 是否强制改密（下次登录）。 */
    private Boolean mustChangePassword;

    /** 锁定截止时间；平台扩展字段，IOT 推送不覆盖。 */
    private LocalDateTime lockedUntil;

    /** 连续失败窗口起点；平台扩展字段。 */
    private LocalDateTime failWindowStart;

    /** 连续登录失败次数；成功登录后清零。 */
    private Integer failCount;

    /** 账号到期日；登录时校验，到期自动锁定。 */
    private LocalDateTime expireAt;

    /** 最近登录时间；由 auth_session 登录成功后回写。 */
    private LocalDateTime lastLoginAt;

    /** 最近登录 IP（IPv4/IPv6）。 */
    private String lastLoginIp;

    /** 乐观锁版本；更新时递增。 */
    @Version
    private Integer version;

    /** 创建时间（UTC），插入自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC），插入/更新自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记；NULL 表示未删除。 */
    private LocalDateTime deletedAt;
}
