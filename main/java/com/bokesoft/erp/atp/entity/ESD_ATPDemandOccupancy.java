package com.bokesoft.erp.atp.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ESD_ATPDemandOccupancy 实体类
 *
 * <p>需求占用记录表</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ESD_ATPDemandOccupancy {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 需求单据类型
     */
    private String demandDocumentType;

    /**
     * 需求单据号
     */
    private String demandDocumentNo;

    /**
     * 需求行号
     */
    private String demandLineNo;

    /**
     * 物料号
     */
    private String materialId;

    /**
     * 工厂ID
     */
    private Long factoryId;

    /**
     * 占用数量
     */
    private BigDecimal occupiedQuantity;

    /**
     * 占用日期
     */
    private Long occupiedDate;

    /**
     * 关联ATP检查ID
     */
    private Long atpCheckId;

    /**
     * 状态: OCCUPIED/RELEASED
     */
    private String status;

    /**
     * 释放时间
     */
    private LocalDateTime releasedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
