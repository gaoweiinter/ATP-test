package com.bokesoft.erp.atp.service;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ATP 库存数据 DTO
 *
 * <p>表示物料在特定存储地点的库存信息</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPStockData {

    /**
     * 物料唯一标识
     */
    private String materialId;

    /**
     * 库存描述
     */
    private String description;

    /**
     * 库存数量
     */
    private BigDecimal quantity;

    /**
     * 存储地点ID
     */
    private Long storageLocationId;
}
