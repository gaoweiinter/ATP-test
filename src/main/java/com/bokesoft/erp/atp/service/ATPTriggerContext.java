package com.bokesoft.erp.atp.service;

import lombok.Data;

/**
 * ATP 触发检查上下文 DTO
 *
 * <p>封装 ATP 触发判断所需的输入参数</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPTriggerContext {

    /**
     * 需求分类.可用性
     * <p>1=触发, 0/其他=不触发</p>
     */
    private Integer requirementClassAvailability;

    /**
     * 计划行类别.可用性
     * <p>1=触发, 0/其他=不触发</p>
     */
    private Integer scheduleLineCategoryAvailability;

    /**
     * 交货项目.ATPcheckoff
     * <p>null/空=触发, 非空=不触发</p>
     */
    private String atpCheckOff;

    /**
     * 单据类型
     * <p>SO=销售订单, DL=外向交货单, PO=生产订单, RS=预留单</p>
     */
    private String documentType;

    /**
     * 操作类型
     * <p>CREATE=创建, MODIFY=修改, DELETE=删除</p>
     */
    private String operationType;
}
