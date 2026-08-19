package com.elink.evco.platform.iam.vo;

/**
 * 用户状态迁移结果。
 *
 * @param userId 用户 ID。
 * @param status 迁移后的账户状态。
 */
public record UserStatusVO(String userId, String status) {}
