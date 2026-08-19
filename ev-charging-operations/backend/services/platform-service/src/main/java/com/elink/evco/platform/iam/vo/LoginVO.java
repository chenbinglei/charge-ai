package com.elink.evco.platform.iam.vo;

import java.util.List;

/**
 * 登录成功响应；部署模式供前端控制 IOT 维护数据域写按钮显隐。
 *
 * @param accessToken 短 TTL 访问令牌（JWT）。
 * @param refreshToken 长 TTL 刷新令牌；一次性使用。
 * @param expiresIn access_token 过期秒数。
 * @param tokenType 令牌类型；固定 Bearer。
 * @param userId 用户 ID。
 * @param displayName 展示姓名。
 * @param roles 角色名称集合。
 * @param deploymentMode 部署模式：IOT_COLLABORATIVE/STANDALONE。
 */
public record LoginVO(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String userId,
        String displayName,
        List<String> roles,
        String deploymentMode) {}
