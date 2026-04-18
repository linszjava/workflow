package com.lin.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假单实体
 */
@Data
@TableName("biz_leave")
public class Leave {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private String userId;

    /** 请假类型: annual-年假 sick-病假 personal-事假 */
    private String leaveType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 请假天数 */
    private Integer days;

    /** 请假事由 */
    private String reason;

    /** 状态: 0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销 */
    private Integer status;

    /** 关联的流程实例ID */
    private String processInstanceId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
