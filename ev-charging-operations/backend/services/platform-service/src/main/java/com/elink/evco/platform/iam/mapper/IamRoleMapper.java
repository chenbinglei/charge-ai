package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.IamRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** IAM 角色表数据访问；含按用户聚合的角色连接查询。 */
@Mapper
public interface IamRoleMapper extends BaseMapper<IamRole> {

    /**
     * 查询用户绑定的全部有效角色（软删过滤）。
     *
     * @param userId 用户 ID。
     * @return 角色集合；无绑定时为空列表。
     */
    @Select(
            "SELECT r.* FROM iam_role r JOIN iam_user_role ur ON ur.role_id = r.id"
                    + " WHERE ur.user_id = #{userId} AND r.deleted_at IS NULL")
    List<IamRole> selectByUserId(@Param("userId") Long userId);
}
