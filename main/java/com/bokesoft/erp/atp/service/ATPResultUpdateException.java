package com.bokesoft.erp.atp.service;

/**
 * ATP 结果更新业务异常类
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public class ATPResultUpdateException extends RuntimeException {

    private final String errorCode;

    public ATPResultUpdateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
