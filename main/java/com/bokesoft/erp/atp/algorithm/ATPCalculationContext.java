package com.bokesoft.erp.atp.algorithm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ATP 计算上下文 DTO
 *
 * <p>包含 ATP 计算所需的输入参数和配置</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class ATPCalculationContext {

    /**
     * 累计计算模式: 0/1/2/3
     * 0: 不累计，已确认数量
     * 1: 累计，已确认数量
     * 2: 创建时用需求数量，修改时用已确认数量
     * 3: 创建时用需求数量，修改时用已确认数量
     */
    private Integer cumulativeParameter;

    /**
     * 是否检查补货提前期
     */
    private Boolean checkReplenishmentLeadTime;

    /**
     * 是否检查存储地点
     */
    private Boolean checkStorageLocation;

    /**
     * 是否包含过去供给
     */
    private Boolean includePastSupply;

    /**
     * 是否包含安全库存
     */
    private Boolean includeSafetyStock;

    /**
     * 计算基准日期 (YYYYMMDD)
     */
    private Long baseDate;

    /**
     * 补货提前期截止日期 (YYYYMMDD)
     */
    private Long replenishmentLeadTimeEndDate = null;

    /**
     * 检查规则: A/B/C/D/E
     * A: Result1 一次性交货
     * B: Result2 全部交货
     * C: Result3 交货建议
     * D/E: A/C 加上对话框逻辑
     */
    private String checkRule;

    /**
     * 请求数量（来自 ATPScheduleDataCollection）
     * 用于结果计算，不参与 ATP 算法本身的计算
     */
    private BigDecimal requestQuantity;

    /**
     * 请求日期（来自 ATPScheduleDataCollection）
     * 用于结果计算
     */
    private Long requestDate;

    /**
     * 请求存储地点（来自 ATPScheduleDataCollection）
     * 用于存储地点级别 ATP 计算
     */
    private String requestStorageLocation;

    // ========== 新增字段：支持存储地点 ATP ==========

    /**
     * 物料信息
     */
    private Material material;

    /**
     * 工厂休息日集合（日期字符串格式：YYYYMMDD）
     */
    private Set<String> holidays = new HashSet<>();

    /**
     * 所有存储地点列表
     */
    private Set<String> storageLocations = new HashSet<>();

    /**
     * 按存储地点分组的数据（用于存储地点级别 ATP 计算）
     * Key: 存储地点ID (String)
     * Value: 该存储地点的数据列表
     */
    private Map<String, List<ATPDataWithCalculation>> storageLocationDataMap = new HashMap<>();

    /**
     * 采购订单处理时间（工作日）
     */
    private Integer poProcessingTime;

    /**
     * 当前计算的存储地点（null 表示工厂层面）
     */
    private String currentStorageLocation;

    /**
     * 是否为子 ATP（存储地点级别 ATP）
     */
    private boolean childATP = false;
}
