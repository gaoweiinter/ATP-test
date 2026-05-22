package com.bokesoft.erp.atp.service;

import lombok.Data;

/**
 * ATP 模型构建上下文 DTO
 *
 * <p>封装 ATP 模型构建所需的输入参数</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPModelContext {

    /**
     * 物料号
     */
    private String materialId;

    /**
     * 工厂ID
     */
    private Long factoryId;

    /**
     * 计算基准日期 (YYYYMMDD)
     */
    private Long baseDate;

    /**
     * 是否检查补货提前期
     */
    private Boolean checkReplenishmentLeadTime = false;

    /**
     * 是否检查存储地点
     */
    private Boolean checkStorageLocation = false;

    /**
     * 是否包含过去供给
     */
    private Boolean includePastSupply = true;

    /**
     * 是否包含安全库存
     */
    private Boolean includeSafetyStock = true;
}
