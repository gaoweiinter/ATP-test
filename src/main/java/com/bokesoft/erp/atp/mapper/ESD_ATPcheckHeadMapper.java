package com.bokesoft.erp.atp.mapper;

import com.bokesoft.erp.atp.entity.ESD_ATPcheckHead;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ESD_ATPcheckHead Mapper 接口
 *
 * @author ATP Engine Team
 * @since Java 17
 */
public interface ESD_ATPcheckHeadMapper {

    /**
     * 根据单据信息查询ATP检查记录
     */
    ESD_ATPcheckHead selectByDocumentKey(@Param("documentType") String documentType,
                                         @Param("documentNo") String documentNo,
                                         @Param("documentLineNo") String documentLineNo);

    /**
     * 根据ID查询
     */
    ESD_ATPcheckHead selectById(@Param("id") Long id);

    /**
     * 插入记录
     */
    int insert(ESD_ATPcheckHead record);

    /**
     * 更新记录
     */
    int update(ESD_ATPcheckHead record);

    /**
     * 根据物料和工厂查询
     */
    List<ESD_ATPcheckHead> selectByMaterialAndFactory(@Param("materialId") String materialId,
                                                      @Param("factoryId") Long factoryId);
}
