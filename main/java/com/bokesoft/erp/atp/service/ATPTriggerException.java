package com.bokesoft.erp.atp.service;

/**
 * ATP 触发检查业务异常
 *
 * <p>当 ATP 触发判断过程中发生业务逻辑错误时抛出</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public class ATPTriggerException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 创建异常
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public ATPTriggerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}
