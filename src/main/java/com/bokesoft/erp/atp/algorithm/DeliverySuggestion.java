package com.bokesoft.erp.atp.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分批交货建议 DTO
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySuggestion {

    /**
     * 交货日期
     */
    private Long date;

    /**
     * 交货数量
     */
    private BigDecimal quantity;
}
