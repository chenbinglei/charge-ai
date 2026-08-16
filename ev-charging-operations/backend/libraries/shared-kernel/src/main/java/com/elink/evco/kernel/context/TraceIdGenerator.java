package com.elink.evco.kernel.context;

import java.util.UUID;

/** 生成不含业务含义的链路追踪标识。 */
public final class TraceIdGenerator {
    /** 禁止创建工具类实例。 */
    private TraceIdGenerator() {}

    /**
     * 生成可在 HTTP 响应、日志、审计和事件中安全传播的追踪标识。
     *
     * @return 长度固定且不含用户或订单信息的追踪标识。
     */
    public static TraceId create() {
        return new TraceId(UUID.randomUUID().toString().replace("-", ""));
    }
}
