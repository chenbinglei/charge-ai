package com.elink.evco.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elink.evco.platform.iam.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌表数据访问；按 SHA-256 摘要定位令牌记录。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {}
