package com.bokesoft.erp.atp.algorithm;

/**
 * ATP 计算异常类
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public class ATPCalculationException extends RuntimeException {

    private final String errorCode;

    public ATPCalculationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
