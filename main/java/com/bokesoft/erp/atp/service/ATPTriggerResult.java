package com.bokesoft.erp.atp.service;

import lombok.Data;

/**
 * ATP 触发检查结果 DTO
 *
 * <p>封装 ATP 触发判断的返回结果</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPTriggerResult {

    /**
     * 是否触发 ATP 检查
     */
    private boolean shouldTrigger;

    /**
     * 不触发原因（shouldTrigger=false 时返回）
     */
    private String reason;

    /**
     * 错误码（异常时返回）
     */
    private String errorCode;

    /**
     * 创建触发结果（所有条件满足）
     *
     * @param reason 触发原因描述
     * @return 触发结果
     */
    public static ATPTriggerResult trigger(String reason) {
        ATPTriggerResult result = new ATPTriggerResult();
        result.setShouldTrigger(true);
        result.setReason(reason);
        return result;
    }

    /**
     * 创建不触发结果（条件不满足）
     *
     * @param reason 不触发原因描述
     * @return 不触发结果
     */
    public static ATPTriggerResult notTrigger(String reason) {
        ATPTriggerResult result = new ATPTriggerResult();
        result.setShouldTrigger(false);
        result.setReason(reason);
        return result;
    }
}
