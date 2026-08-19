package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** IAM 角色（iam_role）：平台级角色归属系统租户；IOT 协同模式由 IOT 推送维护。 */
@Setter
@Getter
@TableName("iam_role")
public class IamRole {

    /** 数据来源：平台维护。 */
    public static final String SOURCE_PLATFORM = "PLATFORM";

    /** 数据来源：IOT 协同模式推送。 */
    public static final String SOURCE_IOT_PUSH = "IOT_PUSH";

    /** 角色 ID（雪花 ID；IOT 模式沿用 IOT 推送 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户；平台级角色使用系统租户。 */
    private Long tenantId;

    /** 角色编码；租户内唯一（生成列 active_code 软删后释放，不在实体映射）。 */
    private String code;

    /** 角色名称。 */
    private String name;

    /** 角色描述。 */
    private String description;

    /** 数据来源：PLATFORM/IOT_PUSH。 */
    private String source;

    /** 创建时间（UTC），插入自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC），插入/更新自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记；NULL 表示未删除。 */
    private LocalDateTime deletedAt;
}
