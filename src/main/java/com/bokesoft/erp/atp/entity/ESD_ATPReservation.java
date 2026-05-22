package com.bokesoft.erp.atp.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ESD_ATPReservation 实体类
 *
 * <p>预留更新记录表</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ESD_ATPReservation {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 预留单号
     */
    private String reservationId;

    /**
     * 预留行号
     */
    private String lineNo;

    /**
     * 物料号
     */
    private String materialId;

    /**
     * 工厂ID
     */
    private Long factoryId;

    /**
     * 已分配数量
     */
    private BigDecimal allocatedQuantity;

    /**
     * 分配日期
     */
    private Long allocationDate;

    /**
     * 分配批次
     */
    private String allocationBatch;

    /**
     * 关联ATP检查ID
     */
    private Long atpCheckId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
