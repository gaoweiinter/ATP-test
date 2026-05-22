package com.bokesoft.erp.atp.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ATP 数据项 DTO
 *
 * <p>表示供给或需求数据的一条记录</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPData {

    /**
     * 物料唯一标识
     */
    private String materialId;

    /**
     * 数据项描述信息
     */
    private String description;

    /**
     * 明细分组行号（对应单据行号）
     */
    private String detailGroup;

    /**
     * 引用的其他明细分组（外向交货单引用销售订单）
     */
    private String referenceDetailGroup;

    /**
     * 需求或供给的日期 (YYYYMMDD)
     */
    private Long date;

    /**
     * MRP元素类型：库存/PO项目/生产订单/订单/交货单/预留
     */
    private String mrpElement;

    /**
     * 方向：1=供给，-1=需求
     */
    private Integer direction;

    /**
     * 收货或需求的数量（始终为正）
     */
    private BigDecimal quantity;

    /**
     * 已确认的承诺数量（仅需求数据使用）
     */
    private BigDecimal confirmedQuantity;

    /**
     * 存储地点标识，-1表示工厂级别
     */
    private Long storageLocationId;
}
