package com.elink.evco.platform.iam.vo;

/**
 * 图形验证码响应；Redis 存储 120 秒，一次性校验。
 *
 * @param captchaId 验证码标识；登录时回传。
 * @param imageBase64 Base64 PNG 图形验证码图片。
 * @param expiresIn 有效秒数。
 */
public record CaptchaVO(String captchaId, String imageBase64, int expiresIn) {}
