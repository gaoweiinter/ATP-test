package com.bokesoft.erp.atp.algorithm;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工厂与存储地点 ATP 合并结果 DTO
 *
 * <p>用于存储地点级别 ATP 计算时，合并工厂层面和存储地点层面的结果</p>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
public class DatePlantStorageATP implements Comparable<DatePlantStorageATP> {

    /**
     * 日期 (YYYYMMDD)
     */
    private Long date;

    /**
     * 工厂层面 ATP 数量
     * -1 表示无数据（需要用前一个日期的值填充）
     */
    private BigDecimal plantQuantity = BigDecimal.valueOf(-1);

    /**
     * 存储地点层面 ATP 数量
     * -1 表示无数据（需要用前一个日期的值填充）
     */
    private BigDecimal storageQuantity = BigDecimal.valueOf(-1);

    /**
     * 实际可用数量 = min(plantQuantity, storageQuantity)
     */
    private BigDecimal realQuantity = BigDecimal.ZERO;

    public DatePlantStorageATP() {
    }

    public DatePlantStorageATP(Long date) {
        this.date = date;
    }

    public DatePlantStorageATP(Long date, BigDecimal plantQuantity, BigDecimal storageQuantity) {
        this.date = date;
        this.plantQuantity = plantQuantity;
        this.storageQuantity = storageQuantity;
    }

    @Override
    public int compareTo(DatePlantStorageATP other) {
        if (this.date == null && other.date == null) return 0;
        if (this.date == null) return -1;
        if (other.date == null) return 1;
        return Long.compare(this.date, other.date);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DatePlantStorageATP other = (DatePlantStorageATP) obj;
        if (this.date == null && other.date == null) return true;
        if (this.date == null || other.date == null) return false;
        return this.date.equals(other.date);
    }

    @Override
    public int hashCode() {
        return date == null ? 0 : date.hashCode();
    }
}
