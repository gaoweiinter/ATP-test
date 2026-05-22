package com.bokesoft.erp.atp.mapper;

import com.bokesoft.erp.atp.entity.ESD_ATPDemandOccupancy;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ESD_ATPDemandOccupancy Mapper 接口
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public interface ESD_ATPDemandOccupancyMapper {

    /**
     * 根据需求单据信息查询
     */
    List<ESD_ATPDemandOccupancy> selectByDemandDocument(@Param("demandDocumentType") String demandDocumentType,
                                                        @Param("demandDocumentNo") String demandDocumentNo,
                                                        @Param("demandLineNo") String demandLineNo);

    /**
     * 根据ATP检查ID查询
     */
    List<ESD_ATPDemandOccupancy> selectByAtpCheckId(@Param("atpCheckId") Long atpCheckId);

    /**
     * 插入记录
     */
    int insert(ESD_ATPDemandOccupancy record);

    /**
     * 更新记录
     */
    int update(ESD_ATPDemandOccupancy record);
}
