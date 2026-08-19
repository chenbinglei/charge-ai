package com.elink.evco.platform.common.config;

/**
 * 平台部署模式（设计包 §8.1）：部署时二选一，运行期不变。
 *
 * <p>IOT_COLLABORATIVE（IOT 协同模式）：平台管理员、权限/角色、租户、场站与设备资产由 IOT
 * 通过 HTTP 推送，平台只读；写 API 返回 DEPLOYMENT_MODE_READONLY。
 * STANDALONE（独立部署模式）：上述数据全部由平台维护，允许写操作。
 */
public enum DeploymentMode {
    /** IOT 协同模式：IOT 推送维护，平台只读。 */
    IOT_COLLABORATIVE,
    /** 独立部署模式：平台全量维护，允许写操作。 */
    STANDALONE;

    /**
     * 从配置值解析部署模式；非法值直接失败，防止拼写错误静默退化为独立模式。
     *
     * @param value 配置中的模式字符串。
     * @return 对应部署模式枚举。
     */
    public static DeploymentMode fromValue(String value) {
        for (DeploymentMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知部署模式：" + value);
    }
}
