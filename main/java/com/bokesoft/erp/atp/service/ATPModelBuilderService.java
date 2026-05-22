package com.bokesoft.erp.atp.service;

import com.bokesoft.erp.atp.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ATP 模型构建服务
 *
 * <p>根据物料和工厂信息，汇总所有相关的供给数据和需求数据，构建 ATP 计算所需的完整模型</p>
 *
 * <p>MRP 元素映射：</p>
 * <ul>
 *   <li>库存 - direction=1</li>
 *   <li>采购订单 - direction=1</li>
 *   <li>生产订单 - direction=1</li>
 *   <li>销售订单 - direction=-1</li>
 *   <li>外向交货单 - direction=-1</li>
 *   <li>预留单 - direction=±1</li>
 * </ul>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Service
public class ATPModelBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ATPModelBuilderService.class);

    /**
     * 供给方向常量
     */
    private static final int DIRECTION_SUPPLY = 1;

    /**
     * 需求方向常量
     */
    private static final int DIRECTION_DEMAND = -1;

    @Autowired(required = false)
    private InventoryMapper inventoryMapper;

    @Autowired(required = false)
    private PurOrderMapper purOrderMapper;

    @Autowired(required = false)
    private ProductionOrderMapper productionOrderMapper;

    @Autowired(required = false)
    private SalesOrderMapper salesOrderMapper;

    @Autowired(required = false)
    private DeliveryNotesMapper deliveryNotesMapper;

    @Autowired(required = false)
    private ReservationMapper reservationMapper;

    /**
     * 构建 ATP 模型
     *
     * @param context 模型构建上下文
     * @return ATP 数据集合
     */
    public ATPDataCollection buildATPModel(ATPModelContext context) {
        // 1. 参数校验（必须首先执行，避免后续日志访问空对象）
        validateContext(context);

        log.info("开始构建 ATP 模型: materialId={}, factoryId={}, baseDate={}",
                context.getMaterialId(), context.getFactoryId(), context.getBaseDate());

        // 2. 构建返回结果
        ATPDataCollection result = new ATPDataCollection();

        // 3. 查询库存数据
        result.setStockDataList(buildStockData(context));

        // 4. 查询供给数据
        result.setSupplyDataList(buildSupplyData(context));

        // 5. 查询需求数据
        result.setDemandDataList(buildDemandData(context));

        log.info("ATP 模型构建完成: stockDataList.size={}, supplyDataList.size={}, demandDataList.size={}",
                result.getStockDataList().size(),
                result.getSupplyDataList().size(),
                result.getDemandDataList().size());

        return result;
    }

    /**
     * 校验上下文参数
     *
     * @param context 模型构建上下文
     */
    private void validateContext(ATPModelContext context) {
        if (context == null) {
            throw new ATPModelBuilderException("MB001", "模型构建上下文不能为空");
        }
        if (context.getMaterialId() == null || context.getMaterialId().trim().isEmpty()) {
            throw new ATPModelBuilderException("MB001", "物料号不能为空");
        }
        if (context.getFactoryId() == null || context.getFactoryId() <= 0) {
            throw new ATPModelBuilderException("MB002", "工厂ID必须大于0");
        }
        if (context.getBaseDate() == null) {
            throw new ATPModelBuilderException("MB003", "基准日期不能为空");
        }
    }

    /**
     * 构建库存数据
     *
     * @param context 模型构建上下文
     * @return 库存数据列表
     */
    private List<ATPStockData> buildStockData(ATPModelContext context) {
        if (inventoryMapper == null) {
            log.warn("InventoryMapper 未配置，返回空列表");
            return List.of();
        }

        List<ATPStockData> stockList = inventoryMapper.selectStockByMaterial(
                context.getMaterialId(), context.getFactoryId());

        log.debug("查询到库存数据: {} 条", stockList.size());
        return stockList;
    }

    /**
     * 构建供给数据（采购订单、生产订单）
     *
     * @param context 模型构建上下文
     * @return 供给数据列表
     */
    private List<ATPData> buildSupplyData(ATPModelContext context) {
        List<ATPData> supplyList = new java.util.ArrayList<>();

        // 查询采购订单（direction=1, mrpElement="PO项目"）
        if (purOrderMapper != null) {
            List<ATPData> poList = purOrderMapper.selectSupplyByMaterial(
                    context.getMaterialId(), context.getFactoryId(), context.getBaseDate());
            for (ATPData data : poList) {
                data.setDirection(DIRECTION_SUPPLY);
                data.setMrpElement("PO项目");
            }
            supplyList.addAll(poList);
            log.debug("查询到采购订单数据: {} 条", poList.size());
        }

        // 查询生产订单（direction=1, mrpElement="生产订单"）
        if (productionOrderMapper != null) {
            List<ATPData> productionList = productionOrderMapper.selectSupplyByMaterial(
                    context.getMaterialId(), context.getFactoryId(), context.getBaseDate());
            for (ATPData data : productionList) {
                data.setDirection(DIRECTION_SUPPLY);
                data.setMrpElement("生产订单");
            }
            supplyList.addAll(productionList);
            log.debug("查询到生产订单数据: {} 条", productionList.size());
        }

        return supplyList;
    }

    /**
     * 构建需求数据（销售订单、外向交货单、预留单）
     *
     * @param context 模型构建上下文
     * @return 需求数据列表
     */
    private List<ATPData> buildDemandData(ATPModelContext context) {
        List<ATPData> demandList = new java.util.ArrayList<>();

        // 查询销售订单（direction=-1, mrpElement="订单"）
        if (salesOrderMapper != null) {
            List<ATPData> soList = salesOrderMapper.selectDemandByMaterial(
                    context.getMaterialId(), context.getFactoryId(), context.getBaseDate());
            for (ATPData data : soList) {
                data.setDirection(DIRECTION_DEMAND);
                data.setMrpElement("订单");
            }
            demandList.addAll(soList);
            log.debug("查询到销售订单数据: {} 条", soList.size());
        }

        // 查询外向交货单（direction=-1, mrpElement="交货单"）
        if (deliveryNotesMapper != null) {
            List<ATPData> dnList = deliveryNotesMapper.selectDemandByMaterial(
                    context.getMaterialId(), context.getFactoryId(), context.getBaseDate());
            for (ATPData data : dnList) {
                data.setDirection(DIRECTION_DEMAND);
                data.setMrpElement("交货单");
            }
            demandList.addAll(dnList);
            log.debug("查询到外向交货单数据: {} 条", dnList.size());
        }

        // 查询预留单（direction=±1, mrpElement="预留单"）
        if (reservationMapper != null) {
            List<ATPData> rsList = reservationMapper.selectByMaterial(
                    context.getMaterialId(), context.getFactoryId(), context.getBaseDate());
            for (ATPData data : rsList) {
                // 预留单的方向由数据本身决定，这里仅设置 MRP 元素类型
                data.setMrpElement("预留单");
            }
            demandList.addAll(rsList);
            log.debug("查询到预留单数据: {} 条", rsList.size());
        }

        return demandList;
    }

    /**
     * 业务异常类
     */
    public static class ATPModelBuilderException extends RuntimeException {
        private final String errorCode;

        public ATPModelBuilderException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
