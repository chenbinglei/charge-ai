package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamPermission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 权限主数据访问；权限集合经 Redis 缓存后供鉴权与 profile 使用。 */
@Mapper
public interface IamPermissionMapper extends BaseMapper<IamPermission> {

    /**
     * 查询用户经「角色-权限直接绑定」与「角色-策略-明细」两条路径并集得到的启用权限编码。
     *
     * @param userId 用户 ID。
     * @return 去重后的权限码集合；无权限时为空列表。
     */
    @Select(
            "SELECT p.code FROM iam_permission p"
                    + " JOIN iam_role_permission rp ON rp.permission_id = p.id"
                    + " JOIN iam_user_role ur ON ur.role_id = rp.role_id"
                    + " WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE' AND p.deleted_at IS NULL"
                    + " UNION"
                    + " SELECT p2.code FROM iam_permission p2"
                    + " JOIN iam_permission_policy_item pi ON pi.permission_id = p2.id"
                    + " JOIN iam_role_policy rpol ON rpol.policy_id = pi.policy_id"
                    + " JOIN iam_user_role ur2 ON ur2.role_id = rpol.role_id"
                    + " WHERE ur2.user_id = #{userId} AND p2.status = 'ACTIVE' AND p2.deleted_at IS NULL")
    List<String> selectActiveCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询全部启用权限编码；用于平台超级管理员全量权限输出。
     *
     * @return 权限码集合。
     */
    @Select("SELECT code FROM iam_permission WHERE status = 'ACTIVE' AND deleted_at IS NULL")
    List<String> selectAllActiveCodes();
}
