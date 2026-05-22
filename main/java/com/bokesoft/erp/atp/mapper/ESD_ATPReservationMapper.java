package com.bokesoft.erp.atp.mapper;

import com.bokesoft.erp.atp.entity.ESD_ATPReservation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ESD_ATPReservation Mapper 接口
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public interface ESD_ATPReservationMapper {

    /**
     * 根据物料和工厂查询预留记录
     */
    List<ESD_ATPReservation> selectByMaterialAndFactory(@Param("materialId") String materialId,
                                                        @Param("factoryId") Long factoryId);

    /**
     * 根据ATP检查ID查询
     */
    List<ESD_ATPReservation> selectByAtpCheckId(@Param("atpCheckId") Long atpCheckId);

    /**
     * 插入记录
     */
    int insert(ESD_ATPReservation record);

    /**
     * 更新记录
     */
    int update(ESD_ATPReservation record);
}
