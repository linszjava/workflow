package com.lin.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_purchase")
public class Purchase {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String userId;
    private String itemName;
    private BigDecimal price;
    private Integer quantity;
    private String reason;
    
    /**
     * 0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销
     */
    private Integer status;
    private String processInstanceId;
    private Date createTime;
    private Date updateTime;
}
