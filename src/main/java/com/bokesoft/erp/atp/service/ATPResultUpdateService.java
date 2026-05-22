package com.bokesoft.erp.atp.service;

import com.bokesoft.erp.atp.entity.ESD_ATPcheckHead;
import com.bokesoft.erp.atp.entity.ESD_ATPDemandOccupancy;
import com.bokesoft.erp.atp.entity.ESD_ATPReservation;
import com.bokesoft.erp.atp.mapper.ESD_ATPcheckHeadMapper;
import com.bokesoft.erp.atp.mapper.ESD_ATPDemandOccupancyMapper;
import com.bokesoft.erp.atp.mapper.ESD_ATPReservationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * ATP 结果更新服务
 *
 * <p>将 ATP 计算结果写入数据库，确保事务一致性</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Service
public class ATPResultUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ATPResultUpdateService.class);

    @Autowired(required = false)
    private ESD_ATPcheckHeadMapper esdAtpcheckHeadMapper;

    @Autowired(required = false)
    private ESD_ATPReservationMapper esdAtpReservationMapper;

    @Autowired(required = false)
    private ESD_ATPDemandOccupancyMapper esdAtpDemandOccupancyMapper;

    /**
     * 更新 ATP 结果
     *
     * @param context 更新上下文
     * @return 更新结果
     */
    @Transactional
    public ATPUpdateResult updateATPResult(ATPUpdateContext context) {
        log.info("开始更新 ATP 结果: documentType={}, documentNo={}, documentLineNo={}",
                context.getDocumentType(), context.getDocumentNo(), context.getDocumentLineNo());

        // 1. 参数校验
        validateContext(context);

        // 2. 查询是否存在已有记录
        ESD_ATPcheckHead existingRecord = findExistingRecord(context);

        // 3. 保存或更新 ESD_ATPcheckHead
        ESD_ATPcheckHead atpCheckHead = saveOrUpdateESD_ATPcheckHead(context, existingRecord);
        Long atpCheckId = atpCheckHead.getId();

        // 4. 更新预留表（如适用）
        if ("RS".equals(context.getDocumentType()) || "PO".equals(context.getDocumentType())) {
            updateReservation(context, atpCheckId);
        }

        // 5. 创建需求占用记录
        createDemandOccupancy(context, atpCheckId);

        log.info("ATP 结果更新成功: atpCheckId={}", atpCheckId);
        return ATPUpdateResult.success(atpCheckId);
    }

    /**
     * 校验上下文参数
     */
    void validateContext(ATPUpdateContext context) {
        if (context.getDocumentType() == null || context.getDocumentType().isEmpty()) {
            throw new ATPResultUpdateException("RU001", "单据类型不能为空");
        }
        Set<String> validDocumentTypes = Set.of("SO", "DL", "PO", "RS");
        if (!validDocumentTypes.contains(context.getDocumentType())) {
            throw new ATPResultUpdateException("RU001", "单据类型无效: " + context.getDocumentType());
        }
        if (context.getDocumentNo() == null || context.getDocumentNo().isEmpty()) {
            throw new ATPResultUpdateException("RU001", "单据号不能为空");
        }
        if (context.getMaterialId() == null || context.getMaterialId().isEmpty()) {
            throw new ATPResultUpdateException("RU002", "物料号不能为空");
        }
        if (context.getFactoryId() == null || context.getFactoryId() <= 0) {
            throw new ATPResultUpdateException("RU003", "工厂ID必须大于0");
        }
        if (context.getResultType() == null || (context.getResultType() != 1 && context.getResultType() != 2 && context.getResultType() != 3)) {
            throw new ATPResultUpdateException("RU006", "结果类型无效: " + context.getResultType());
        }
        if (context.getConfirmedQuantity() == null) {
            throw new ATPResultUpdateException("RU004", "确认数量不能为空");
        }
        if (context.getConfirmedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ATPResultUpdateException("RU004", "确认数量不能为负数");
        }
        if (context.getConfirmedQuantity().scale() > 3) {
            throw new ATPResultUpdateException("RU004", "确认数量精度不能超过3位小数");
        }
        if (context.getConfirmedDate() == null || context.getConfirmedDate() < 19000101L || context.getConfirmedDate() > 21001231L) {
            throw new ATPResultUpdateException("RU005", "确认日期格式错误: " + context.getConfirmedDate());
        }
    }

    /**
     * 查询已有记录
     */
    ESD_ATPcheckHead findExistingRecord(ATPUpdateContext context) {
        if (esdAtpcheckHeadMapper == null) {
            return null;
        }
        return esdAtpcheckHeadMapper.selectByDocumentKey(
                context.getDocumentType(),
                context.getDocumentNo(),
                context.getDocumentLineNo()
        );
    }

    /**
     * 保存或更新 ESD_ATPcheckHead
     */
    ESD_ATPcheckHead saveOrUpdateESD_ATPcheckHead(ATPUpdateContext context, ESD_ATPcheckHead existingRecord) {
        ESD_ATPcheckHead record = new ESD_ATPcheckHead();

        if (existingRecord != null) {
            // 更新已有记录
            record.setId(existingRecord.getId());
            record.setStatus("CONFIRMED");
        } else {
            // 插入新记录
            record.setStatus("PENDING");
        }

        record.setDocumentType(context.getDocumentType());
        record.setDocumentNo(context.getDocumentNo());
        record.setDocumentLineNo(context.getDocumentLineNo());
        record.setMaterialId(context.getMaterialId());
        record.setFactoryId(context.getFactoryId());
        record.setConfirmedDate(context.getConfirmedDate());
        record.setConfirmedQuantity(context.getConfirmedQuantity());
        record.setResultType(context.getResultType());
        record.setResultTypeName(context.getResultTypeName());
        record.setCheckRule(context.getCheckRule());
        record.setCumulativeParameter(context.getCumulativeParameter());
        record.setUpdatedAt(LocalDateTime.now());

        if (existingRecord != null) {
            esdAtpcheckHeadMapper.update(record);
        } else {
            record.setCreatedAt(LocalDateTime.now());
            esdAtpcheckHeadMapper.insert(record);
        }

        return record;
    }

    /**
     * 更新预留表
     */
    void updateReservation(ATPUpdateContext context, Long atpCheckId) {
        if (esdAtpReservationMapper == null) {
            return;
        }

        // 查询相关预留
        List<ESD_ATPReservation> reservations = esdAtpReservationMapper.selectByMaterialAndFactory(
                context.getMaterialId(), context.getFactoryId());

        for (ESD_ATPReservation reservation : reservations) {
            // 计算分配数量
            BigDecimal allocateQty = context.getConfirmedQuantity().min(reservation.getAllocatedQuantity());
            reservation.setAllocatedQuantity(reservation.getAllocatedQuantity().subtract(allocateQty));
            reservation.setAtpCheckId(atpCheckId);
            reservation.setAllocationDate(context.getConfirmedDate());
            esdAtpReservationMapper.update(reservation);
        }
    }

    /**
     * 创建需求占用记录
     */
    void createDemandOccupancy(ATPUpdateContext context, Long atpCheckId) {
        if (esdAtpDemandOccupancyMapper == null) {
            return;
        }

        ESD_ATPDemandOccupancy occupancy = new ESD_ATPDemandOccupancy();
        occupancy.setDemandDocumentType(context.getDocumentType());
        occupancy.setDemandDocumentNo(context.getDocumentNo());
        occupancy.setDemandLineNo(context.getDocumentLineNo());
        occupancy.setMaterialId(context.getMaterialId());
        occupancy.setFactoryId(context.getFactoryId());
        occupancy.setOccupiedQuantity(context.getConfirmedQuantity());
        occupancy.setOccupiedDate(context.getConfirmedDate());
        occupancy.setAtpCheckId(atpCheckId);
        occupancy.setStatus("OCCUPIED");
        occupancy.setCreatedAt(LocalDateTime.now());

        esdAtpDemandOccupancyMapper.insert(occupancy);
    }
}
