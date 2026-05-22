package com.bokesoft.erp.atp.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ATP 触发检查服务
 *
 * <p>根据单据类型和操作类型判断是否触发 ATP 计算，并检查触发条件</p>
 *
 * <p>业务规则：</p>
 * <ul>
 *   <li>ATP 检查仅在以下全部条件满足时触发：
 *     <ul>
 *       <li>需求分类.可用性 = 1</li>
 *       <li>计划行类别.可用性 = 1</li>
 *       <li>交货项目.ATPcheckoff 为空或 null</li>
 *     </ul>
 *   </li>
 *   <li>ATP 时机判断：
 *     <ul>
 *       <li>销售订单(SO): CREATE=触发, MODIFY=触发, DELETE=不触发</li>
 *       <li>外向交货单(DL): CREATE=触发, MODIFY=触发, DELETE=触发</li>
 *       <li>生产订单(PO): CREATE=触发, MODIFY=触发, DELETE=不触发</li>
 *       <li>预留单(RS): CREATE=触发, MODIFY=触发, DELETE=触发</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author ATP Engine Team
 * @since Java 17
 */
@Service
public class ATPTriggerCheckService {

    private static final Logger log = LoggerFactory.getLogger(ATPTriggerCheckService.class);

    /**
     * 单据类型常量
     */
    private static final String DOC_TYPE_SO = "SO";
    private static final String DOC_TYPE_DL = "DL";
    private static final String DOC_TYPE_PO = "PO";
    private static final String DOC_TYPE_RS = "RS";

    /**
     * 操作类型常量
     */
    private static final String OP_TYPE_CREATE = "CREATE";
    private static final String OP_TYPE_MODIFY = "MODIFY";
    private static final String OP_TYPE_DELETE = "DELETE";

    /**
     * 判断是否应触发 ATP 检查
     *
     * @param context 触发检查上下文
     * @return 触发检查结果
     */
    public ATPTriggerResult shouldTriggerATP(ATPTriggerContext context) {
        // 1. 参数校验（必须最先执行）
        validateContext(context);

        log.debug("ATP trigger check input: documentType={}, operationType={}",
                context.getDocumentType(), context.getOperationType());

        // 2. ATP 时机判断
        if (!shouldCalculateATP(context.getDocumentType(), context.getOperationType())) {
            log.info("ATP trigger result: shouldTrigger=false, reason={}",
                    getNotTriggerReason(context.getDocumentType(), context.getOperationType()));
            return ATPTriggerResult.notTrigger(
                    getNotTriggerReason(context.getDocumentType(), context.getOperationType()));
        }

        // 3. 触发条件判断
        if (!checkTriggerConditions(context)) {
            String reason = getConditionNotMetReason(context);
            log.info("ATP trigger result: shouldTrigger=false, reason={}", reason);
            return ATPTriggerResult.notTrigger(reason);
        }

        log.info("ATP trigger result: shouldTrigger=true, reason=所有触发条件满足");
        return ATPTriggerResult.trigger("所有触发条件满足");
    }

    /**
     * 校验上下文参数
     *
     * @param context 触发检查上下文
     * @throws ATPTriggerException 参数不完整或不支持
     */
    private void validateContext(ATPTriggerContext context) {
        if (context == null) {
            throw new ATPTriggerException("TC001", "触发上下文参数不完整：context 为空");
        }

        // 校验单据类型
        if (context.getDocumentType() == null ||
                (!DOC_TYPE_SO.equals(context.getDocumentType()) &&
                        !DOC_TYPE_DL.equals(context.getDocumentType()) &&
                        !DOC_TYPE_PO.equals(context.getDocumentType()) &&
                        !DOC_TYPE_RS.equals(context.getDocumentType()))) {
            throw new ATPTriggerException("TC002",
                    "单据类型不支持：" + context.getDocumentType() + "，仅支持 SO/DL/PO/RS");
        }

        // 校验操作类型
        if (context.getOperationType() == null ||
                (!OP_TYPE_CREATE.equals(context.getOperationType()) &&
                        !OP_TYPE_MODIFY.equals(context.getOperationType()) &&
                        !OP_TYPE_DELETE.equals(context.getOperationType()))) {
            throw new ATPTriggerException("TC003",
                    "操作类型不支持：" + context.getOperationType() + "，仅支持 CREATE/MODIFY/DELETE");
        }
    }

    /**
     * 判断是否应执行 ATP 计算
     *
     * <p>销售订单删除和生产订单删除不触发 ATP 计算</p>
     *
     * @param documentType  单据类型
     * @param operationType 操作类型
     * @return true=应执行 ATP 计算, false=不执行
     */
    private boolean shouldCalculateATP(String documentType, String operationType) {
        // 销售订单删除不触发
        if (DOC_TYPE_SO.equals(documentType) && OP_TYPE_DELETE.equals(operationType)) {
            return false;
        }
        // 生产订单删除不触发
        if (DOC_TYPE_PO.equals(documentType) && OP_TYPE_DELETE.equals(operationType)) {
            return false;
        }
        // 其他组合都触发
        return true;
    }

    /**
     * 检查触发条件
     *
     * <p>所有条件都满足才返回 true</p>
     *
     * @param context 触发检查上下文
     * @return true=所有条件满足, false=任一条件不满足
     */
    private boolean checkTriggerConditions(ATPTriggerContext context) {
        // 条件1: 需求分类.可用性 = 1
        boolean condition1 = Objects.equals(context.getRequirementClassAvailability(), 1);

        // 条件2: 计划行类别.可用性 = 1
        boolean condition2 = Objects.equals(context.getScheduleLineCategoryAvailability(), 1);

        // 条件3: 交货项目.ATPcheckoff 为空
        boolean condition3 = isBlank(context.getAtpCheckOff());

        return condition1 && condition2 && condition3;
    }

    /**
     * 判断字符串是否为空白（null 或空字符串）
     *
     * @param str 待检查字符串
     * @return true=空白, false=非空白
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 获取不触发原因（基于单据类型和操作类型）
     *
     * @param documentType  单据类型
     * @param operationType 操作类型
     * @return 不触发原因
     */
    private String getNotTriggerReason(String documentType, String operationType) {
        if (DOC_TYPE_SO.equals(documentType) && OP_TYPE_DELETE.equals(operationType)) {
            return "销售订单删除不触发ATP检查";
        }
        if (DOC_TYPE_PO.equals(documentType) && OP_TYPE_DELETE.equals(operationType)) {
            return "生产订单删除不触发ATP检查";
        }
        return "不满足ATP计算时机";
    }

    /**
     * 获取触发条件不满足的具体原因
     *
     * @param context 触发检查上下文
     * @return 不满足的原因
     */
    private String getConditionNotMetReason(ATPTriggerContext context) {
        if (!Objects.equals(context.getRequirementClassAvailability(), 1)) {
            return "需求分类.可用性≠1";
        }
        if (!Objects.equals(context.getScheduleLineCategoryAvailability(), 1)) {
            return "计划行类别.可用性≠1";
        }
        if (!isBlank(context.getAtpCheckOff())) {
            return "交货项目.ATPcheckoff非空";
        }
        return "触发条件不满足";
    }
}
