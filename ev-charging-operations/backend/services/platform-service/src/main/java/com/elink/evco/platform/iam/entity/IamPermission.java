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
 * 权限主数据（iam_permission）：资源 + 读/写两档（code=resource:read/write），
 * 与 IOT linkos 共用编码体系；关闭态功能权限初始化为 INACTIVE。
 */
@Setter
@Getter
@TableName("iam_permission")
public class IamPermission {

    /** 状态：启用。 */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** 状态：停用（关闭态功能）。 */
    public static final String STATUS_INACTIVE = "INACTIVE";

    /** 权限 ID（种子数据使用保留段，运行时为雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 权限编码，格式 resource:action。 */
    private String code;

    /** 权限中文名称。 */
    private String name;

    /** 资源标识，如 iam_user。 */
    private String resource;

    /** 动作：read/write。 */
    private String action;

    /** 权限层级：PLATFORM/TENANT/ENTERPRISE。 */
    private String scope;

    /** 对应三级菜单路由；平台扩展字段。 */
    private String menuPath;

    /** 权限描述。 */
    private String description;

    /** 是否内置权限；内置权限不可删除。 */
    private Boolean isBuiltin;

    /** 状态：ACTIVE/INACTIVE。 */
    private String status;

    /** 首次交付工作周，如 W2。 */
    private String phase;

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
