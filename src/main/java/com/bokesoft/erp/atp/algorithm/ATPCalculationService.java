package com.bokesoft.erp.atp.algorithm;

import com.bokesoft.erp.atp.service.ATPDataCollection;
import com.bokesoft.erp.atp.service.ATPStockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ATP 计算服务
 *
 * <p>执行累计 ATP 计算，支持离散 ATP/累积 ATP 两种计算模式</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Service
public class ATPCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ATPCalculationService.class);

    private static final BigDecimal INFINITY = new BigDecimal("999999999999");

    /**
     * 执行 ATP 计算
     *
     * @param dataCollection ATP 数据集合
     * @param context 计算上下文
     * @return ATP 计算结果
     */
    public ATPResult calculate(ATPDataCollection dataCollection, ATPCalculationContext context) {
        log.info("开始 ATP 计算: cumulativeParameter={}, checkRule={}, baseDate={}",
                context.getCumulativeParameter(), context.getCheckRule(), context.getBaseDate());

        // 1. 参数校验
        validateContext(context);

        // 2. 数据排序
        List<ATPDataWithCalculation> sortedDataList = sortData(dataCollection);

        // 3. 设置计算数量
        setCalcQuantity(sortedDataList, context);

        // 4. 计算正差和负差
        calcZhengchaFucha(sortedDataList);

        // 5. 计算工厂层面累计 ATP
        BigDecimal stockQuantity = calculateStockQuantity(dataCollection);
        calcCumulativeATPQuantity(sortedDataList, stockQuantity, context.getReplenishmentLeadTimeEndDate());

        // 6. 生成结果
        ATPResult result = generateResult(sortedDataList, context, stockQuantity);

        log.info("ATP 计算完成: resultType={}", result.getResultType());
        return result;
    }

    /**
     * 校验计算上下文参数
     */
    private void validateContext(ATPCalculationContext context) {
        if (context.getCumulativeParameter() == null ||
            context.getCumulativeParameter() < 0 || context.getCumulativeParameter() > 3) {
            throw new ATPCalculationException("CA001", "累计参数无效: " + context.getCumulativeParameter());
        }
        if (context.getCheckRule() == null || context.getCheckRule().isEmpty()) {
            throw new ATPCalculationException("CA002", "检查规则不能为空");
        }
        if (context.getBaseDate() == null) {
            throw new ATPCalculationException("CA003", "基准日期不能为空");
        }
    }

    /**
     * 计算库存数量
     */
    private BigDecimal calculateStockQuantity(ATPDataCollection dataCollection) {
        if (dataCollection.getStockDataList() == null || dataCollection.getStockDataList().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return dataCollection.getStockDataList().stream()
                .map(stock -> stock.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 数据排序
     * - 第一排序：日期升序
     * - 第二排序：方向倒序（供给 1 在前，需求 -1 在后）
     * - 第三排序：MRP 元素字母序
     *
     * <p>注意：库存数据会被添加为第一条记录（日期为基准日期，数量为库存总和）</p>
     */
    List<ATPDataWithCalculation> sortData(ATPDataCollection dataCollection) {
        List<ATPDataWithCalculation> result = new ArrayList<>();

        // 添加库存数据作为第一条记录（工厂层面 ATP = 库存总和）
        // 库存行不参与 calcZhengchaFucha 计算，calcQuantity 设为 0
        if (dataCollection.getStockDataList() != null && !dataCollection.getStockDataList().isEmpty()) {
            BigDecimal totalStock = dataCollection.getStockDataList().stream()
                    .map(ATPStockData::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ATPDataWithCalculation stockData = new ATPDataWithCalculation();
            stockData.setMaterialId("STOCK");
            stockData.setDetailGroup("STOCK");
            stockData.setDate(0L); // 库存行日期设为0，排在最前面
            stockData.setDirection(1); // 供给
            stockData.setQuantity(totalStock);
            stockData.setCalcQuantity(BigDecimal.ZERO); // 库存行不参与 calcZhengchaFucha
            stockData.setMrpElement("库存");
            stockData.setStorageLocationId(-1L);
            stockData.setPositiveDiff(totalStock); // 库存的正差就是库存数量
            result.add(stockData);
        }

        // 转换供给数据
        if (dataCollection.getSupplyDataList() != null) {
            for (var data : dataCollection.getSupplyDataList()) {
                ATPDataWithCalculation calcData = convertToCalculationData(data);
                calcData.setCalcQuantity(data.getQuantity());
                result.add(calcData);
            }
        }

        // 转换需求数据
        if (dataCollection.getDemandDataList() != null) {
            for (var data : dataCollection.getDemandDataList()) {
                ATPDataWithCalculation calcData = convertToCalculationData(data);
                // 需求数据的 calcQuantity 会由 setCalcQuantity 处理（取负数）
                result.add(calcData);
            }
        }

        // 排序：第一按日期升序，第二按方向降序（供给1在需求-1之前），第三按MRP元素升序
        result.sort(Comparator
                .comparing(ATPDataWithCalculation::getDate)
                .thenComparing((a, b) -> {
                    // 方向降序：1 (供给) > -1 (需求)
                    if (a.getDirection() > b.getDirection()) return -1;
                    if (a.getDirection() < b.getDirection()) return 1;
                    return 0;
                })
                .thenComparing(ATPDataWithCalculation::getMrpElement, Comparator.nullsLast(String::compareTo))
        );

        return result;
    }

    /**
     * 转换 ATPData 为 ATPDataWithCalculation
     */
    private ATPDataWithCalculation convertToCalculationData(com.bokesoft.erp.atp.service.ATPData data) {
        ATPDataWithCalculation calcData = new ATPDataWithCalculation();
        calcData.setMaterialId(data.getMaterialId());
        calcData.setDetailGroup(data.getDetailGroup());
        calcData.setDate(data.getDate());
        calcData.setDirection(data.getDirection());
        calcData.setQuantity(data.getQuantity());
        calcData.setMrpElement(data.getMrpElement());
        calcData.setStorageLocationId(data.getStorageLocationId());
        calcData.setConfirmedQuantity(data.getConfirmedQuantity());
        return calcData;
    }

    /**
     * 设置计算数量（根据累计参数和操作类型）
     *
     * <p>注意：
     * - 库存数据（mrpElement="库存"）的 calcQuantity 由 sortData 设置，跳过此方法
     * - 需求数据（direction=-1）的 calcQuantity 会取负数
     * - 这是为了在 calcZhengchaFucha 中直接累加计算</p>
     */
    void setCalcQuantity(List<ATPDataWithCalculation> dataList, ATPCalculationContext context) {
        for (ATPDataWithCalculation data : dataList) {
            // 跳过库存数据
            if ("库存".equals(data.getMrpElement())) {
                continue;
            }

            if (data.getDirection() == 1) {
                // 供给数据：直接使用数量（正数）
                data.setCalcQuantity(data.getQuantity());
            } else {
                // 需求数据：根据累计参数和操作类型
                Integer cumParam = context.getCumulativeParameter();
                String operationType = data.getOperationType();
                BigDecimal calcQty;

                switch (cumParam) {
                    case 0:
                    case 1:
                        // 对于需求数据，使用原始需求数量（ReceivedRequiredQuantity）
                        // 而不是已确认数量（ConfirmedQuantity）
                        calcQty = data.getQuantity();
                        break;
                    case 2:
                    case 3:
                        if ("CREATE".equals(operationType)) {
                            calcQty = data.getQuantity();
                        } else {
                            calcQty = data.getConfirmedQuantity() != null ?
                                    data.getConfirmedQuantity() : BigDecimal.ZERO;
                        }
                        break;
                    default:
                        calcQty = data.getQuantity();
                }

                // 对需求数据取负数（出库取负数）
                data.setCalcQuantity(calcQty.negate());
            }
        }
    }

    /**
     * 计算正差和负差（从后往前遍历）
     *
     * <p>算法：
     * - 直接累加 calcQuantity（需求为负数，供给为正数）
     * - 结果 >= 0：正差 = 结果，负差 = 0，清零
     * - 结果 < 0：正差 = 0，负差 = 结果（负数）
     */
    public void calcZhengchaFucha(List<ATPDataWithCalculation> dataList) {
        BigDecimal remaining = BigDecimal.ZERO;

        // 从后往前遍历
        for (int i = dataList.size() - 1; i >= 0; i--) {
            ATPDataWithCalculation data = dataList.get(i);

            // 直接累加（需求数据 calcQuantity 已经是负数）
            remaining = remaining.add(data.getCalcQuantity());

            if (remaining.compareTo(BigDecimal.ZERO) >= 0) {
                data.setPositiveDiff(remaining);
                data.setNegativeDiff(BigDecimal.ZERO);
                remaining = BigDecimal.ZERO; // 清零
            } else {
                data.setPositiveDiff(BigDecimal.ZERO);
                data.setNegativeDiff(remaining);
            }
        }
    }

    /**
     * 计算累计 ATP 数量
     */
    public void calcCumulativeATPQuantity(List<ATPDataWithCalculation> dataList,
                                          BigDecimal stockQuantity,
                                          Long rltEndDate) {
        if (dataList.isEmpty()) {
            return;
        }

        // 工厂库存的累计 ATP = 库存数量 + 第一条数据的负差
        BigDecimal firstNegativeDiff = dataList.get(0).getNegativeDiff();
        if (firstNegativeDiff == null) {
            firstNegativeDiff = BigDecimal.ZERO;
        }

        BigDecimal cumulativeATP = stockQuantity.add(firstNegativeDiff);

        // 遍历计算每条数据的累计 ATP
        for (int i = 0; i < dataList.size(); i++) {
            ATPDataWithCalculation data = dataList.get(i);

            // 上一条的累计 ATP
            BigDecimal lastCumulativeATP;
            if (i == 0) {
                // 第0条 = 初始累计 ATP + 第0条的正差
                lastCumulativeATP = cumulativeATP.add(data.getPositiveDiff() != null ? data.getPositiveDiff() : BigDecimal.ZERO);
            } else {
                // 第i条 = 第i-1条的累计 ATP + 第i条的正差
                lastCumulativeATP = dataList.get(i - 1).getCumulativeAtpQuantity()
                        .add(data.getPositiveDiff() != null ? data.getPositiveDiff() : BigDecimal.ZERO);
            }

            // 如果检查补货提前期，且日期 >= 截止日期，则设为无穷大
            if (rltEndDate != null && data.getDate() >= rltEndDate) {
                data.setCumulativeAtpQuantity(INFINITY);
            } else {
                data.setCumulativeAtpQuantity(lastCumulativeATP);
            }
        }
    }

    /**
     * 生成结果（根据 checkRule）
     */
    private ATPResult generateResult(List<ATPDataWithCalculation> dataList,
                                    ATPCalculationContext context,
                                    BigDecimal stockQuantity) {
        String checkRule = context.getCheckRule();

        switch (checkRule) {
            case "A":
                return generateResult1(dataList, context);
            case "B":
                return generateResult2(dataList, context);
            case "C":
                return generateResult3(dataList, context);
            default:
                return generateResult3(dataList, context);
        }
    }

    /**
     * Result1 - 一次性交货
     * 查找请求日期当天的累计ATP
     */
    private ATPResult generateResult1(List<ATPDataWithCalculation> dataList,
                                    ATPCalculationContext context) {
        Long requestDate = context.getRequestDate();
        BigDecimal requestQuantity = context.getRequestQuantity();
        Long rltEndDate = context.getReplenishmentLeadTimeEndDate();
        boolean checkRlt = Boolean.TRUE.equals(context.getCheckReplenishmentLeadTime());

        // RLT 特殊处理：如果启用 RLT，且 max(scheduleDate, calculateDate) >= RLT截止日期
        if (checkRlt && rltEndDate != null && Math.max(requestDate, context.getBaseDate()) >= rltEndDate) {
            ATPResult result = new ATPResult();
            result.setResultType(1);
            result.setResultTypeName("一次性交货");
            result.setConfirmedDate(Math.max(requestDate, context.getBaseDate()));
            result.setConfirmedQuantity(requestQuantity);
            return result;
        }

        // 找到请求日期当天的累计ATP（可能在请求日期之前，因为计划数据不参与计算）
        BigDecimal cumulativeAtp = BigDecimal.ZERO;
        for (ATPDataWithCalculation data : dataList) {
            // 找到请求日期当天或之前的最近一条数据的累计ATP
            if (data.getDate() <= requestDate) {
                cumulativeAtp = data.getCumulativeAtpQuantity();
            } else {
                break;
            }
        }

        ATPResult result = new ATPResult();
        result.setResultType(1);
        result.setResultTypeName("一次性交货");
        result.setConfirmedDate(requestDate);

        // 确认数量 = min(累计ATP, 请求数量)
        if (cumulativeAtp.compareTo(requestQuantity) >= 0) {
            result.setConfirmedQuantity(requestQuantity);
        } else {
            result.setConfirmedQuantity(cumulativeAtp);
        }

        return result;
    }

    /**
     * Result2 - 全部交货
     * 找到首个累计ATP >= 请求数量的日期
     *
     * <p>特殊处理：如果启用 RLT 检查且日期已超过 RLT 截止日期，
     * 则在 RLT 截止日期交货（因为过了截止日期后，累计ATP = 无穷大）</p>
     */
    private ATPResult generateResult2(List<ATPDataWithCalculation> dataList,
                                     ATPCalculationContext context) {
        Long requestDate = context.getRequestDate();
        BigDecimal requestQuantity = context.getRequestQuantity();
        Long rltEndDate = context.getReplenishmentLeadTimeEndDate();
        boolean checkRlt = Boolean.TRUE.equals(context.getCheckReplenishmentLeadTime());

        ATPResult result = new ATPResult();
        result.setResultType(2);
        result.setResultTypeName("全部交货");

        // 如果启用 RLT 检查，且请求日期已超过 RLT 截止日期
        if (checkRlt && rltEndDate != null && requestDate >= rltEndDate) {
            result.setConfirmedDate(requestDate);
            result.setConfirmedQuantity(requestQuantity);
            return result;
        }

        // 遍历数据，找到首个累计ATP >= 请求数量的日期
        for (ATPDataWithCalculation data : dataList) {
            // RLT 处理：如果启用 RLT 且当前数据日期已超过 RLT 截止日期
            if (checkRlt && rltEndDate != null && data.getDate() >= rltEndDate) {
                // 使用 RLT 截止日期作为结果日期
                result.setConfirmedDate(rltEndDate);
                result.setConfirmedQuantity(requestQuantity);
                return result;
            }

            if (data.getCumulativeAtpQuantity().compareTo(requestQuantity) >= 0) {
                result.setConfirmedDate(Math.max(requestDate, data.getDate()));
                result.setConfirmedQuantity(requestQuantity);
                return result;
            }
        }

        // 未找到满足条件的日期 - "不能满足"
        result.setConfirmedDate(0L);
        result.setConfirmedQuantity(BigDecimal.ZERO);
        return result;
    }

    /**
     * Result3 - 交货建议
     * 优先使用正差数量进行分批交货
     *
     * <p>算法逻辑（基于 calcCumulativeATPQuantity 的结果）：
     * 1. 请求日期当天的累计 ATP 是可供交货的数量
     * 2. 如果请求数量 <= 累计 ATP，全部在请求日期交货
     * 3. 如果请求数量 > 累计 ATP，部分在请求日期交货，剩余在后续日期交货
     */
    private ATPResult generateResult3(List<ATPDataWithCalculation> dataList,
                                     ATPCalculationContext context) {
        Long requestDate = context.getRequestDate();
        BigDecimal requestQuantity = context.getRequestQuantity();

        ATPResult result = new ATPResult();
        result.setResultType(3);
        result.setResultTypeName("交货建议");

        // 找到请求日期当天的累计 ATP
        BigDecimal cumulativeAtpAtRequestDate = BigDecimal.ZERO;
        for (ATPDataWithCalculation data : dataList) {
            if (data.getDate() <= requestDate) {
                cumulativeAtpAtRequestDate = data.getCumulativeAtpQuantity();
            } else {
                break;
            }
        }

        List<DeliverySuggestion> suggestions = new ArrayList<>();

        // 在请求日期交货：min(累计ATP, 请求数量)
        BigDecimal atRequestDate = cumulativeAtpAtRequestDate.min(requestQuantity);
        if (atRequestDate.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(new DeliverySuggestion(requestDate, atRequestDate));
        }

        // 如果还有剩余需求，在后续日期交货
        BigDecimal remaining = requestQuantity.subtract(atRequestDate);
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // 从请求日期之后的数据中，找到正差来满足剩余需求
            for (ATPDataWithCalculation data : dataList) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (data.getDate() <= requestDate) {
                    continue;
                }
                if (data.getDirection() != 1) {
                    continue;
                }
                BigDecimal zhengcha = data.getPositiveDiff();
                if (zhengcha == null || zhengcha.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal allocate = zhengcha.min(remaining);
                suggestions.add(new DeliverySuggestion(data.getDate(), allocate));
                remaining = remaining.subtract(allocate);
            }
        }

        result.setDeliverySuggestions(suggestions);
        return result;
    }

    // ========== 工厂日历方法 ==========

    /**
     * 计算工作日偏移（跳过休息日）
     *
     * @param startDate 起始日期 (YYYYMMDD)
     * @param offset    工作日偏移天数
     * @param holidays  休息日集合
     * @return 计算后的日期 (YYYYMMDD)
     */
    public Long getDate4PlantCalendar(Long startDate, int offset, Set<String> holidays) {
        if (startDate == null || offset <= 0) {
            return startDate;
        }

        long currentDate = startDate;
        int remainingOffset = offset;

        while (remainingOffset > 0) {
            currentDate = dateIntAdd("d", 1, currentDate);
            String dateStr = String.valueOf(currentDate);
            // 如果不是休息日，才算一个工作日
            if (holidays == null || !holidays.contains(dateStr)) {
                remainingOffset--;
            }
        }

        return currentDate;
    }

    /**
     * 计算自然日偏移（不跳过休息日）
     *
     * @param startDate 起始日期 (YYYYMMDD)
     * @param offset    日历天数偏移
     * @return 计算后的日期 (YYYYMMDD)
     */
    public Long getDate4Calendar(Long startDate, int offset) {
        if (startDate == null) {
            return null;
        }
        return dateIntAdd("d", offset, startDate);
    }

    /**
     * 日期加法计算
     *
     * @param unit  单位：d=天, m=月, y=年
     * @param value 偏移值
     * @param date  日期 (YYYYMMDD)
     * @return 计算后的日期
     */
    private Long dateIntAdd(String unit, int value, Long date) {
        int year = (int) (date / 10000);
        int month = (int) ((date % 10000) / 100);
        int day = (int) (date % 100);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, day); // 月份从0开始

        switch (unit) {
            case "d":
                cal.add(java.util.Calendar.DAY_OF_MONTH, value);
                break;
            case "m":
                cal.add(java.util.Calendar.MONTH, value);
                break;
            case "y":
                cal.add(java.util.Calendar.YEAR, value);
                break;
            default:
                break;
        }

        year = cal.get(java.util.Calendar.YEAR);
        month = cal.get(java.util.Calendar.MONTH) + 1;
        day = cal.get(java.util.Calendar.DAY_OF_MONTH);

        return (long) (year * 10000 + month * 100 + day);
    }

    // ========== 补货提前期计算 ==========

    /**
     * 计算补货提前期截止日期
     *
     * @param material     物料信息
     * @param calculateDate 计算基准日期
     * @param holidays     休息日集合
     * @return 补货提前期截止日期
     */
    public Long calcReplenishmentLeadTimeEndDate(Material material, Long calculateDate, Set<String> holidays) {
        if (material == null) {
            return null;
        }

        // 采购件 (PurType="F")
        if (material.isPurchasing()) {
            // 截止日期 = 基准日期 + 采购处理时间(工作日) + 计划交货时间(自然日) + 收货处理时间(工作日)
            int poProcessingTime = material.getPoProcessingTime() != null ? material.getPoProcessingTime() : 0;
            int plannedDeliveryTime = material.getPlannedDeliveryTime() != null ? material.getPlannedDeliveryTime() : 0;
            int goodsReceiptProcessingTime = material.getGoodsReceiptProcessingTime() != null ? material.getGoodsReceiptProcessingTime() : 0;

            Long date = calculateDate;
            if (poProcessingTime > 0) {
                date = getDate4PlantCalendar(date, poProcessingTime, holidays);
            }
            if (plannedDeliveryTime > 0) {
                date = getDate4Calendar(date, plannedDeliveryTime);
            }
            if (goodsReceiptProcessingTime > 0) {
                date = getDate4PlantCalendar(date, goodsReceiptProcessingTime, holidays);
            }
            return date;
        }

        // 生产件 (PurType="E")
        if (material.isProduction()) {
            int totalRlt = material.getTotalReplenishmentLeadTime() != null ? material.getTotalReplenishmentLeadTime() : 0;

            if (totalRlt > 0) {
                // 使用总补货提前期
                return getDate4PlantCalendar(calculateDate, totalRlt, holidays);
            } else {
                // 使用 收货处理时间 + 自制生产时间
                int goodsReceiptProcessingTime = material.getGoodsReceiptProcessingTime() != null ? material.getGoodsReceiptProcessingTime() : 0;
                int inHouseProductionTime = material.getInHouseProductionTime() != null ? material.getInHouseProductionTime() : 0;
                int total = goodsReceiptProcessingTime + inHouseProductionTime;
                return getDate4PlantCalendar(calculateDate, total, holidays);
            }
        }

        return null;
    }

    // ========== 存储地点 ATP 计算 ==========

    /**
     * 按存储地点分组数据
     */
    private void groupDataByStorageLocation(ATPDataCollection dataCollection, ATPCalculationContext context) {
        Map<String, List<ATPDataWithCalculation>> storageLocationDataMap = new HashMap<>();

        // 收集所有存储地点
        Set<String> storageLocations = new java.util.HashSet<>();

        // 收集库存数据的存储地点
        if (dataCollection.getStockDataList() != null) {
            for (ATPStockData stock : dataCollection.getStockDataList()) {
                String storageLoc = String.valueOf(stock.getStorageLocationId());
                if (!"-1".equals(storageLoc) && !"0".equals(storageLoc)) {
                    storageLocations.add(storageLoc);
                }
            }
        }

        // 收集供需数据的存储地点
        if (dataCollection.getSupplyDataList() != null) {
            for (var data : dataCollection.getSupplyDataList()) {
                String storageLoc = String.valueOf(data.getStorageLocationId());
                if (!"-1".equals(storageLoc) && !"0".equals(storageLoc)) {
                    storageLocations.add(storageLoc);
                }
            }
        }

        if (dataCollection.getDemandDataList() != null) {
            for (var data : dataCollection.getDemandDataList()) {
                String storageLoc = String.valueOf(data.getStorageLocationId());
                if (!"-1".equals(storageLoc) && !"0".equals(storageLoc)) {
                    storageLocations.add(storageLoc);
                }
            }
        }

        // 初始化每个存储地点的数据列表
        for (String loc : storageLocations) {
            storageLocationDataMap.put(loc, new ArrayList<>());
        }

        // 分组库存数据
        if (dataCollection.getStockDataList() != null) {
            for (ATPStockData stock : dataCollection.getStockDataList()) {
                String storageLoc = String.valueOf(stock.getStorageLocationId());
                if ("-1".equals(storageLoc) || "0".equals(storageLoc)) {
                    continue; // 工厂层面库存已经合到工厂ATP
                }
                if (storageLocationDataMap.containsKey(storageLoc)) {
                    ATPDataWithCalculation calcData = new ATPDataWithCalculation();
                    calcData.setMaterialId(stock.getMaterialId());
                    calcData.setDetailGroup("STOCK");
                    calcData.setDate(0L); // 库存行日期设为0
                    calcData.setDirection(1); // 供给
                    calcData.setQuantity(stock.getQuantity());
                    calcData.setCalcQuantity(BigDecimal.ZERO);
                    calcData.setMrpElement("库存");
                    calcData.setStorageLocationId(stock.getStorageLocationId());
                    calcData.setPositiveDiff(stock.getQuantity());
                    storageLocationDataMap.get(storageLoc).add(calcData);
                }
            }
        }

        // 分组供给数据
        if (dataCollection.getSupplyDataList() != null) {
            for (var data : dataCollection.getSupplyDataList()) {
                String storageLoc = String.valueOf(data.getStorageLocationId());
                if (!"-1".equals(storageLoc) && !"0".equals(storageLoc) && storageLocationDataMap.containsKey(storageLoc)) {
                    ATPDataWithCalculation calcData = convertToCalculationData(data);
                    calcData.setCalcQuantity(data.getQuantity());
                    storageLocationDataMap.get(storageLoc).add(calcData);
                }
            }
        }

        // 分组需求数据
        if (dataCollection.getDemandDataList() != null) {
            for (var data : dataCollection.getDemandDataList()) {
                String storageLoc = String.valueOf(data.getStorageLocationId());
                if (!"-1".equals(storageLoc) && !"0".equals(storageLoc) && storageLocationDataMap.containsKey(storageLoc)) {
                    ATPDataWithCalculation calcData = convertToCalculationData(data);
                    storageLocationDataMap.get(storageLoc).add(calcData);
                }
            }
        }

        context.setStorageLocations(storageLocations);
        context.setStorageLocationDataMap(storageLocationDataMap);
    }

    /**
     * 计算存储地点级别 ATP
     */
    private Map<String, ATPResult> calculateStorageLocationATP(
            ATPDataCollection dataCollection,
            ATPCalculationContext context,
            Map<String, List<ATPDataWithCalculation>> storageLocationDataMap) {

        Map<String, ATPResult> storageLocationResults = new HashMap<>();

        for (Map.Entry<String, List<ATPDataWithCalculation>> entry : storageLocationDataMap.entrySet()) {
            String storageLocationId = entry.getKey();
            List<ATPDataWithCalculation> storageDataList = entry.getValue();

            if (storageDataList.isEmpty()) {
                continue;
            }

            // 对该存储地点的数据进行排序
            storageDataList.sort(Comparator
                    .comparing(ATPDataWithCalculation::getDate)
                    .thenComparing((a, b) -> {
                        if (a.getDirection() > b.getDirection()) return -1;
                        if (a.getDirection() < b.getDirection()) return 1;
                        return 0;
                    })
                    .thenComparing(ATPDataWithCalculation::getMrpElement, Comparator.nullsLast(String::compareTo))
            );

            // 设置计算数量
            ATPCalculationContext slContext = createSubContext(context);
            slContext.setCurrentStorageLocation(storageLocationId);
            slContext.setChildATP(true);
            setCalcQuantity(storageDataList, slContext);

            // 计算正差负差
            calcZhengchaFucha(storageDataList);

            // 计算库存数量（该存储地点的库存总和）
            BigDecimal stockQuantity = storageDataList.stream()
                    .filter(d -> "库存".equals(d.getMrpElement()))
                    .map(ATPDataWithCalculation::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算累计 ATP
            calcCumulativeATPQuantity(storageDataList, stockQuantity, context.getReplenishmentLeadTimeEndDate());

            // 生成结果
            ATPResult slResult = generateResult(storageDataList, context, stockQuantity);
            slResult.setStorageLocationId(storageLocationId);
            storageLocationResults.put(storageLocationId, slResult);
        }

        return storageLocationResults;
    }

    /**
     * 创建子 ATP 的计算上下文
     */
    private ATPCalculationContext createSubContext(ATPCalculationContext parentContext) {
        ATPCalculationContext subContext = new ATPCalculationContext();
        subContext.setCumulativeParameter(parentContext.getCumulativeParameter());
        subContext.setCheckReplenishmentLeadTime(parentContext.getCheckReplenishmentLeadTime());
        subContext.setCheckStorageLocation(false); // 子 ATP 不再递归检查存储地点
        subContext.setIncludePastSupply(parentContext.getIncludePastSupply());
        subContext.setIncludeSafetyStock(parentContext.getIncludeSafetyStock());
        subContext.setBaseDate(parentContext.getBaseDate());
        subContext.setReplenishmentLeadTimeEndDate(parentContext.getReplenishmentLeadTimeEndDate());
        subContext.setCheckRule(parentContext.getCheckRule());
        subContext.setRequestQuantity(parentContext.getRequestQuantity());
        subContext.setRequestDate(parentContext.getRequestDate());
        subContext.setMaterial(parentContext.getMaterial());
        subContext.setHolidays(parentContext.getHolidays());
        subContext.setPoProcessingTime(parentContext.getPoProcessingTime());
        subContext.setChildATP(true);
        return subContext;
    }

    /**
     * 合并工厂层面和存储地点层面的 ATP 结果
     *
     * @param plantResult          工厂层面结果
     * @param storageLocationResults 存储地点层面结果 Map
     * @return 合并后的结果
     */
    private ATPResult mergeWithStorageLocationATP(
            ATPResult plantResult,
            Map<String, ATPResult> storageLocationResults,
            ATPCalculationContext context) {

        if (storageLocationResults == null || storageLocationResults.isEmpty()) {
            plantResult.setMerged(false);
            return plantResult;
        }

        // 获取请求日期的存储地点ID
        // 这里简化处理：使用请求数据中指定的存储地点
        String requestStorageLocation = context.getRequestStorageLocation();
        if (requestStorageLocation == null) {
            plantResult.setMerged(false);
            return plantResult;
        }

        ATPResult storageResult = storageLocationResults.get(requestStorageLocation);
        if (storageResult == null) {
            plantResult.setMerged(false);
            return plantResult;
        }

        // 合并 Result3：取 min(工厂, 存储地点)
        List<DeliverySuggestion> plantSuggestions = plantResult.getDeliverySuggestions();
        List<DeliverySuggestion> storageSuggestions = storageResult.getDeliverySuggestions();

        List<DeliverySuggestion> mergedSuggestions = mergeDeliverySuggestions(plantSuggestions, storageSuggestions);

        ATPResult mergedResult = new ATPResult();
        mergedResult.setResultType(3);
        mergedResult.setResultTypeName("交货建议(合并)");
        mergedResult.setDeliverySuggestions(mergedSuggestions);
        mergedResult.setMerged(true);
        mergedResult.setStorageLocationId(requestStorageLocation);

        // 计算 Result1：取合并后第一个日期的可用数量
        if (!mergedSuggestions.isEmpty()) {
            Long firstDate = mergedSuggestions.get(0).getDate();
            BigDecimal firstQty = mergedSuggestions.get(0).getQuantity();

            ATPResult result1 = ATPResult.result1(firstDate, firstQty);
            mergedResult.setResultType(1);
            mergedResult.setResultTypeName("一次性交货(合并)");
            mergedResult.setConfirmedDate(firstDate);
            mergedResult.setConfirmedQuantity(firstQty);
        }

        // 计算 Result2：如果所有日期都不能满足请求，返回 date=0, qty=0
        BigDecimal totalAvailable = mergedSuggestions.stream()
                .map(DeliverySuggestion::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ATPResult result2;
        if (totalAvailable.compareTo(context.getRequestQuantity()) >= 0) {
            // 能全部交货
            result2 = ATPResult.result2(mergedSuggestions.get(mergedSuggestions.size() - 1).getDate(),
                    context.getRequestQuantity());
        } else {
            // 不能全部交货
            result2 = ATPResult.result2(0L, BigDecimal.ZERO);
        }

        // 返回 Result2 作为主要结果标识
        mergedResult.setResultType(2);
        mergedResult.setResultTypeName("全部交货(合并)");
        mergedResult.setConfirmedDate(result2.getConfirmedDate());
        mergedResult.setConfirmedQuantity(result2.getConfirmedQuantity());

        return mergedResult;
    }

    /**
     * 合并两个交货建议列表
     * 对每个日期，取 min(工厂ATP, 存储地点ATP)
     */
    private List<DeliverySuggestion> mergeDeliverySuggestions(
            List<DeliverySuggestion> plantSuggestions,
            List<DeliverySuggestion> storageSuggestions) {

        if (plantSuggestions == null || plantSuggestions.isEmpty()) {
            return storageSuggestions != null ? storageSuggestions : new ArrayList<>();
        }
        if (storageSuggestions == null || storageSuggestions.isEmpty()) {
            return plantSuggestions;
        }

        // 转换为日期->数量 map
        Map<Long, BigDecimal> plantMap = new HashMap<>();
        for (DeliverySuggestion s : plantSuggestions) {
            plantMap.put(s.getDate(), s.getQuantity());
        }

        Map<Long, BigDecimal> storageMap = new HashMap<>();
        for (DeliverySuggestion s : storageSuggestions) {
            storageMap.put(s.getDate(), s.getQuantity());
        }

        // 合并：取 min
        Set<Long> allDates = new java.util.HashSet<>();
        allDates.addAll(plantMap.keySet());
        allDates.addAll(storageMap.keySet());

        List<Long> sortedDates = new ArrayList<>(allDates);
        Collections.sort(sortedDates);

        List<DeliverySuggestion> merged = new ArrayList<>();
        for (Long date : sortedDates) {
            BigDecimal plantQty = plantMap.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal storageQty = storageMap.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal mergedQty = plantQty.min(storageQty);
            if (mergedQty.compareTo(BigDecimal.ZERO) > 0) {
                merged.add(new DeliverySuggestion(date, mergedQty));
            }
        }

        return merged;
    }

    // ========== reInit / recalculate ==========

    /**
     * 重新初始化并计算
     * 清除所有计算的中间结果，然后重新执行计算
     *
     * @param dataCollection ATP 数据集合
     * @param context        计算上下文
     * @return ATP 计算结果
     */
    public ATPResult recalculate(ATPDataCollection dataCollection, ATPCalculationContext context) {
        // 清除中间结果
        clearCalculationResults(dataCollection);

        // 重新计算
        return calculate(dataCollection, context);
    }

    /**
     * 清除计算的中间结果
     */
    private void clearCalculationResults(ATPDataCollection dataCollection) {
        // 清除库存数据的中间结果
        if (dataCollection.getStockDataList() != null) {
            for (ATPStockData stock : dataCollection.getStockDataList()) {
                // ATPStockData 可能有 reset 方法，这里简化处理
            }
        }

        // 清除供给数据的中间结果
        if (dataCollection.getSupplyDataList() != null) {
            for (var data : dataCollection.getSupplyDataList()) {
                // 如果 ATPData 有中间结果字段需要清除
            }
        }

        // 清除需求数据的中间结果
        if (dataCollection.getDemandDataList() != null) {
            for (var data : dataCollection.getDemandDataList()) {
                // 如果 ATPData 有中间结果字段需要清除
            }
        }
    }
}
