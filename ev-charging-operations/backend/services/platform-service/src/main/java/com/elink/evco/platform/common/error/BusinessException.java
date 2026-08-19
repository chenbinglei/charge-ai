package com.elink.evco.platform.common.error;

/**
 * 业务失败异常；由全局异常处理器统一转换为稳定 ApiResponse 外观。
 *
 * <p>仅承载已登记的稳定错误码；技术栈异常（SQL、序列化等）不允许包装成本异常，
 * 由处理器统一归并为 INTERNAL_ERROR，避免向调用方泄露内部细节。
 */
public class BusinessException extends RuntimeException {
    /** 稳定错误码；不允许为 null。 */
    private final PlatformErrorCode errorCode;

    /**
     * 以默认中文说明抛出业务失败。
     *
     * @param errorCode 已登记的稳定错误码。
     */
    public BusinessException(PlatformErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 以场景化中文说明抛出业务失败；说明不得包含敏感材料。
     *
     * @param errorCode 已登记的稳定错误码。
     * @param message 面向调用方的场景化中文说明。
     */
    public BusinessException(PlatformErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** @return 已登记的稳定错误码。 */
    public PlatformErrorCode errorCode() {
        return errorCode;
    }
}
