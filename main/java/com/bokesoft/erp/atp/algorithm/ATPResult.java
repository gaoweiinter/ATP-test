package com.bokesoft.erp.atp.algorithm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ATP 计算结果 DTO
 *
 * <p>包含 3 种结果类型：一次性交货、全部交货、交货建议</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPResult {

    /**
     * 结果类型: 1=一次性交货, 2=全部交货, 3=交货建议
     */
    private Integer resultType;

    /**
     * 结果类型名称
     */
    private String resultTypeName;

    /**
     * 确认日期 (Result1/Result2)
     */
    private Long confirmedDate;

    /**
     * 确认数量 (Result1/Result2)
     */
    private BigDecimal confirmedQuantity;

    /**
     * 分批交货建议 (Result3)
     */
    private List<DeliverySuggestion> deliverySuggestions = new ArrayList<>();

    /**
     * 错误码（异常时返回）
     */
    private String errorCode;

    /**
     * 错误消息（异常时返回）
     */
    private String errorMessage;

    /**
     * 存储地点ID（用于存储地点级别 ATP 结果标识）
     * null 表示工厂层面
     */
    private String storageLocationId;

    /**
     * 是否已合并（工厂+存储地点）
     */
    private boolean merged;

    /**
     * 创建 Result1 - 一次性交货
     */
    public static ATPResult result1(Long confirmedDate, BigDecimal confirmedQuantity) {
        ATPResult result = new ATPResult();
        result.setResultType(1);
        result.setResultTypeName("一次性交货");
        result.setConfirmedDate(confirmedDate);
        result.setConfirmedQuantity(confirmedQuantity);
        return result;
    }

    /**
     * 创建 Result2 - 全部交货
     */
    public static ATPResult result2(Long confirmedDate, BigDecimal confirmedQuantity) {
        ATPResult result = new ATPResult();
        result.setResultType(2);
        result.setResultTypeName("全部交货");
        result.setConfirmedDate(confirmedDate);
        result.setConfirmedQuantity(confirmedQuantity);
        return result;
    }

    /**
     * 创建 Result3 - 交货建议
     */
    public static ATPResult result3(List<DeliverySuggestion> deliverySuggestions) {
        ATPResult result = new ATPResult();
        result.setResultType(3);
        result.setResultTypeName("交货建议");
        result.setDeliverySuggestions(deliverySuggestions);
        return result;
    }
}
