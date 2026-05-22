package com.bokesoft.erp.atp.service;

import com.bokesoft.erp.atp.algorithm.DeliverySuggestion;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ATP 结果更新上下文 DTO
 *
 * <p>包含 ATP 结果更新所需的 12 个输入字段</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPUpdateContext {

    /**
     * 单据类型: SO=销售订单, DL=外向交货单, PO=生产订单, RS=预留单
     */
    private String documentType;

    /**
     * 单据号
     */
    private String documentNo;

    /**
     * 行号
     */
    private String documentLineNo;

    /**
     * 物料号
     */
    private String materialId;

    /**
     * 工厂ID
     */
    private Long factoryId;

    /**
     * 确认日期 (YYYYMMDD)
     */
    private Long confirmedDate;

    /**
     * 确认数量
     */
    private BigDecimal confirmedQuantity;

    /**
     * 结果类型: 1=一次性交货, 2=全部交货, 3=交货建议
     */
    private Integer resultType;

    /**
     * 结果类型名称
     */
    private String resultTypeName;

    /**
     * 分批交货建议 (Result3)
     */
    private List<DeliverySuggestion> deliverySuggestions;

    /**
     * 检查规则: A/B/C/D/E
     */
    private String checkRule;

    /**
     * 累计参数: 0/1/2/3
     */
    private Integer cumulativeParameter;
}
