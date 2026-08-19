package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 刷新令牌（refresh_token）：原文不落库，仅存 SHA-256 摘要；一次性使用后轮换。
 */
@Setter
@Getter
@TableName("refresh_token")
public class RefreshToken {

    /** 状态：可用。 */
    public static final String STATUS_ACTIVE = "active";

    /** 状态：已使用（一次性）。 */
    public static final String STATUS_USED = "used";

    /** 状态：已撤销。 */
    public static final String STATUS_REVOKED = "revoked";

    /** 刷新令牌 ID（雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属会话 ID（auth_session）；会话撤销时级联失效。 */
    private Long sessionId;

    /** 用户 ID（冗余 iam_user.id）。 */
    private Long userId;

    /** 刷新令牌 SHA-256 摘要；唯一。 */
    private String tokenHash;

    /** 状态：active/used/revoked。 */
    private String status;

    /** 过期时间（UTC）。 */
    private LocalDateTime expiresAt;

    /** 创建时间（UTC），插入自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC），插入/更新自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
