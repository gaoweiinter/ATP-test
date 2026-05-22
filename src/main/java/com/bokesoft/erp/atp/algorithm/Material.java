package com.bokesoft.erp.atp.algorithm;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料参数 DTO
 *
 * <p>包含物料的采购类型、提前期等参数，用于 ATP 计算</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class Material {

    /**
     * 物料号
     */
    private String materialId;

    /**
     * 采购类型
     * F = 外部采购（采购件）
     * E = 内部生产（生产件）
     */
    private String purType;

    /**
     * 总计补货提前时间（考虑工厂日历）Total RLT
     * 对内部生产可用
     * 对于生产件没有设置了此时间，则考虑 inHouseProductionTime + goodsReceiptProcessingTime
     */
    private Integer totalReplenishmentLeadTime;

    /**
     * 计划交货时间（考虑自然日历）
     * 对采购件使用
     */
    private Integer plannedDeliveryTime;

    /**
     * 收货处理时间（考虑工厂日历）
     */
    private Integer goodsReceiptProcessingTime;

    /**
     * 自制生产时间（考虑工厂日历）
     */
    private Integer inHouseProductionTime;

    /**
     * BOM 基准数量
     */
    private BigDecimal bomBaseQuantity;

    /**
     * BOM 数量
     */
    private BigDecimal bomQuantity;

    /**
     * 安全库存
     */
    private BigDecimal safeStockQuantity;

    /**
     * 采购处理时间（考虑工厂日历）
     */
    private Integer poProcessingTime;

    public Material() {
    }

    /**
     * 判断是否为采购件
     */
    public boolean isPurchasing() {
        return "F".equals(purType);
    }

    /**
     * 判断是否为生产件
     */
    public boolean isProduction() {
        return "E".equals(purType);
    }
}
