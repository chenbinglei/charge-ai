package com.elink.evco.web.error;

/**
 * 平台服务稳定错误码登记表；取值与 contracts/openapi/error-codes-v1.yaml 逐一对应。
 *
 * <p>新增业务错误码必须先在契约文件登记，再补充到本枚举；禁止在响应中输出未登记的临时码。
 */
public enum PlatformErrorCode {
    /** 操作成功。 */
    SUCCESS(200, "操作成功"),
    /** 参数、格式或受控字段校验失败。 */
    VALIDATION_ERROR(400, "参数、格式或受控字段校验失败"),
    /** 未登录或令牌无效。 */
    UNAUTHENTICATED(401, "未登录或令牌无效"),
    /** 无页面、数据或业务操作权限。 */
    FORBIDDEN(403, "无页面、数据或业务操作权限"),
    /** IOT 协同模式下该数据域只读。 */
    DEPLOYMENT_MODE_READONLY(403, "当前为 IOT 协同模式，该数据由 IOT 平台维护"),
    /** SSO 票据无效、已使用或已过期。 */
    SSO_TICKET_INVALID(401, "SSO 免登录票据无效、已使用或已过期"),
    /** SSO 用户未被 IOT 推送或当前不可用。 */
    SSO_USER_NOT_FOUND(403, "SSO 免登录用户未由 IOT 推送到平台或当前不可用"),
    /** 资源不存在或对调用方不可见。 */
    RESOURCE_NOT_FOUND(404, "资源不存在或对调用方不可见"),
    /** 相同幂等键对应不同请求摘要。 */
    IDEMPOTENCY_CONFLICT(409, "相同幂等键对应不同请求摘要"),
    /** 乐观锁版本已变化。 */
    VERSION_CONFLICT(409, "乐观锁版本已变化，刷新后重新提交"),
    /** 当前状态不允许该转换。 */
    STATE_CONFLICT(409, "当前状态不允许该转换"),
    /** 管理用户不存在或不可见。 */
    IAM_USER_NOT_FOUND(404, "管理用户不存在或对调用方不可见"),
    /** 用户名租户范围内已存在。 */
    IAM_USERNAME_DUPLICATE(409, "用户名在租户范围内已存在"),
    /** 最后一个可用平台超级管理员保护。 */
    IAM_LAST_ADMIN_PROTECTED(409, "不得停用最后一个可用平台超级管理员"),
    /** 越权授予角色。 */
    IAM_ROLE_OUT_OF_SCOPE(403, "操作者不可授予超出可授予范围的角色"),
    /** 渠道身份冲突，需人工处置。 */
    IAM_CHANNEL_IDENTITY_CONFLICT(409, "渠道身份已关联其他个人主档或需人工处置"),
    /** 令牌无效或已撤销。 */
    AUTH_TOKEN_INVALID(401, "令牌无效或已撤销"),
    /** 令牌已过期。 */
    AUTH_TOKEN_EXPIRED(401, "令牌已过期"),
    /** 刷新令牌已使用或已撤销。 */
    AUTH_REFRESH_TOKEN_USED(401, "refresh_token 已使用或已撤销"),
    /** 未预期系统错误；响应不暴露内部详情。 */
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试");

    /** HTTP 响应状态码，与契约登记一致。 */
    private final int httpStatus;

    /** 面向调用方的中文默认说明；可被业务异常覆盖为更具体的场景描述。 */
    private final String defaultMessage;

    /**
     * 登记一个稳定错误码。
     *
     * @param httpStatus HTTP 响应状态码。
     * @param defaultMessage 面向调用方的中文默认说明。
     */
    PlatformErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /**
     * @return HTTP 响应状态码。
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * @return 面向调用方的中文默认说明。
     */
    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * @return 契约中登记的稳定结果码字符串（枚举名）。
     */
    public String code() {
        return name();
    }
}
