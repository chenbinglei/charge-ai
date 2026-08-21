package com.elink.evco.platform.iam.service;

import com.elink.evco.kernel.api.ApiResponse;
import java.util.function.Supplier;

/**
 * 幂等键服务：以请求体指纹做 Redis SET NX 去重；同键同指纹重放首个成功响应， 同键异指纹返回 IDEMPOTENCY_CONFLICT；Redis 故障时降级放行（幂等非安全控制）。
 */
public interface IdempotencyService {

    /**
     * 幂等执行模板：命中缓存重放首个成功响应，未命中执行业务后缓存响应。
     *
     * <p>控制器只声明作用域、幂等键、请求体与业务供应器，编排细节收敛到本服务。
     *
     * @param <T> 业务返回数据类型。
     * @param scope 幂等作用域。
     * @param idempotencyKey 幂等键；空白表示跳过幂等直接执行。
     * @param requestBody 请求体（指纹比对）。
     * @param supplier 业务执行供应器。
     * @return 首次执行或重放的成功响应。
     */
    <T> ApiResponse<T> execute(
            String scope, String idempotencyKey, Object requestBody, Supplier<T> supplier);
}
