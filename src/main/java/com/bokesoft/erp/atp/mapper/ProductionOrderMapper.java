package com.bokesoft.erp.atp.mapper;

import com.bokesoft.erp.atp.service.ATPData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 生产订单 Mapper 接口
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public interface ProductionOrderMapper {

    /**
     * 根据物料查询生产订单供给数据
     *
     * @param materialId 物料号
     * @param factoryId  工厂ID
     * @param baseDate   基准日期
     * @return 生产订单数据列表（direction=1 供给）
     */
    List<ATPData> selectSupplyByMaterial(@Param("materialId") String materialId,
                                          @Param("factoryId") Long factoryId,
                                          @Param("baseDate") Long baseDate);
}
