package com.bokesoft.erp.atp.service;

import lombok.Data;

/**
 * ATP 结果更新结果 DTO
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPUpdateResult {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 更新记录数
     */
    private Integer updateCount;

    /**
     * ATP检查记录ID
     */
    private Long atpCheckId;

    /**
     * 错误码（失败时返回）
     */
    private String errorCode;

    /**
     * 错误消息（失败时返回）
     */
    private String errorMessage;

    /**
     * 成功消息
     */
    private String message;

    /**
     * 创建成功结果
     */
    public static ATPUpdateResult success(Long atpCheckId) {
        ATPUpdateResult result = new ATPUpdateResult();
        result.setSuccess(true);
        result.setUpdateCount(1);
        result.setAtpCheckId(atpCheckId);
        result.setMessage("ATP 结果更新成功");
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ATPUpdateResult failure(String errorCode, String errorMessage) {
        ATPUpdateResult result = new ATPUpdateResult();
        result.setSuccess(false);
        result.setUpdateCount(0);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
