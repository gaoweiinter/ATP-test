package com.bokesoft.erp.atp.mapper;

import com.bokesoft.erp.atp.service.ATPStockData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存数据 Mapper 接口
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public interface InventoryMapper {

    /**
     * 根据物料查询库存数据
     *
     * @param materialId 物料号
     * @param factoryId  工厂ID
     * @return 库存数据列表
     */
    List<ATPStockData> selectStockByMaterial(@Param("materialId") String materialId,
                                              @Param("factoryId") Long factoryId);

    /**
     * 根据物料和存储地点查询库存数据
     *
     * @param materialId        物料号
     * @param factoryId          工厂ID
     * @param storageLocationId  存储地点ID
     * @return 库存数据列表
     */
    List<ATPStockData> selectStockByMaterialAndStorageLocation(@Param("materialId") String materialId,
                                                                @Param("factoryId") Long factoryId,
                                                                @Param("storageLocationId") Long storageLocationId);
}
