package com.bokesoft.erp.atp.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ATP 数据集合 DTO
 *
 * <p>封装 ATP 计算所需的供给数据、需求数据和库存数据</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPDataCollection {

    /**
     * 供给数据列表
     */
    private List<ATPData> supplyDataList = new ArrayList<>();

    /**
     * 需求数据列表
     */
    private List<ATPData> demandDataList = new ArrayList<>();

    /**
     * 库存数据列表
     */
    private List<ATPStockData> stockDataList = new ArrayList<>();
}
