package com.lin.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 业务人员/组织架构实体类
 */
@Data
@TableName("biz_user")
public class BizUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号ID (对应 flowable 的 user_id)
     */
    private String userId;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 角色/岗位
     */
    private String roleName;

    /**
     * 直属领导账号ID (manager_id)
     */
    private String managerId;

    private Date createTime;
}
