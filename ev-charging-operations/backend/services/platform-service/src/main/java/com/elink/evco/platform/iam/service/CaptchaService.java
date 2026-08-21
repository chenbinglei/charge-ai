package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.vo.CaptchaVO;

/** 图形验证码服务：4-6 位字母数字（剔除易混淆字符），Redis 存储 120 秒， GETDEL 一次性消费；大小写不敏感。 */
public interface CaptchaService {

    /**
     * 生成图形验证码：返回标识与 Base64 PNG 图片，内容小写落 Redis。
     *
     * @return 验证码响应（captchaId、图片、有效秒数）。
     */
    CaptchaVO generate();

    /**
     * 一次性校验验证码；GETDEL 原子读取并删除，比对大小写不敏感。
     *
     * @param captchaId 验证码标识。
     * @param input 用户输入内容。
     * @return true 表示校验通过（验证码同时被消费）。
     */
    boolean verify(String captchaId, String input);
}
