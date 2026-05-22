package com.bokesoft.erp.atp.algorithm;

import com.bokesoft.erp.atp.service.ATPData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 带计算属性的 ATP 数据 DTO
 *
 * <p>扩展 ATPData，增加计算所需的中间结果字段</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ATPDataWithCalculation extends ATPData {

    /**
     * 计算数量（根据累计参数处理后）
     */
    private BigDecimal calcQuantity;

    /**
     * 正差（剩余可用量）
     */
    private BigDecimal positiveDiff;

    /**
     * 负差（不足量）
     */
    private BigDecimal negativeDiff;

    /**
     * 工厂层面累计 ATP 数量
     */
    private BigDecimal cumulativeAtpQuantity;

    /**
     * 无累计 ATP 数量
     */
    private BigDecimal noCumulativeAtpQuantity;

    /**
     * 操作类型：CREATE/MODIFY
     */
    private String operationType;

    public ATPDataWithCalculation() {
        super();
        this.calcQuantity = BigDecimal.ZERO;
        this.positiveDiff = BigDecimal.ZERO;
        this.negativeDiff = BigDecimal.ZERO;
        this.cumulativeAtpQuantity = BigDecimal.ZERO;
        this.noCumulativeAtpQuantity = BigDecimal.ZERO;
    }
}
