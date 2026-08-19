package com.elink.evco.platform.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户-角色绑定（iam_user_role）：多对多纯关联表；绑定变更须写审计。
 */
@Setter
@Getter
@TableName("iam_user_role")
public class IamUserRole {

    /** 绑定 ID（雪花 ID）。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID（iam_user）。 */
    private Long userId;

    /** 角色 ID（iam_role）。 */
    private Long roleId;

    /** 数据来源：PLATFORM/IOT_PUSH。 */
    private String source;
}
