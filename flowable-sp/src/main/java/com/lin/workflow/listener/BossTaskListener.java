package com.lin.workflow.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lin.workflow.entity.BizUser;
import com.lin.workflow.mapper.BizUserMapper;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 智能指派监听器（用于动态分配部门经理节点）
 */
@Component("bossTaskListener")
public class BossTaskListener implements TaskListener {

    @Autowired
    private BizUserMapper bizUserMapper;

    @Override
    public void notify(DelegateTask delegateTask) {
        // 1. 获取申请人（即流程启动时绑定的变量）
        String initiator = (String) delegateTask.getVariable("initiator");

        // 2. 真实查库操作：查询组织架构，获得他的真实直属上级 (managerId)
        BizUser initiatorInfo = bizUserMapper.selectOne(new LambdaQueryWrapper<BizUser>().eq(BizUser::getUserId, initiator));
        
        String bossId = null;
        if (initiatorInfo != null) {
            bossId = initiatorInfo.getManagerId();
        }

        // 如果找不到上级记录（比如老板自己发单，或是异常数据），降级为直接推给整个候选组，由人抢单
        if (bossId != null && !bossId.isEmpty()) {
            // 3. 动态指派任务办理人
            System.out.println("[DB智能派单] 发现 " + initiator + " 的申请单，系统查库匹配并已自动指派给该人的领导：" + bossId);
            delegateTask.setAssignee(bossId);
        } else {
            System.out.println("[DB智能派单] 未找到 " + initiator + " 的固定上司，兜底扔进【部门经理池】由人抢单。");
            delegateTask.addCandidateGroup("deptManager");
        }
    }
}
