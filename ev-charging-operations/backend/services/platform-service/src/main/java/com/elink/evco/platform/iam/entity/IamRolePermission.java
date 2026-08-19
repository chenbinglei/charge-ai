package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 角色-权限直接绑定（iam_role_permission）：纯关联表，禁用通过解绑实现。 */
@Setter
@Getter
@TableName("iam_role_permission")
public class IamRolePermission {

    /** 绑定 ID（雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色 ID（iam_role）。 */
    private Long roleId;

    /** 权限 ID（iam_permission）。 */
    private Long permissionId;
}
