package com.lin.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.workflow.entity.Purchase;
import com.lin.workflow.mapper.PurchaseMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseMapper purchaseMapper;

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;

    /**
     * 提交采购申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchase(Purchase purchase) {
        // 1. 保存业务数据
        purchase.setStatus(1); // 审批中
        purchaseMapper.insert(purchase);

        // 2. 启动流程实例，传入业务参数
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", purchase.getUserId());
        // 重点：在这里注入会签人员集合
        variables.put("expertList", Arrays.asList("expert1", "expert2", "expert3"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "purchaseProcess",
                purchase.getId().toString(),
                variables
        );

        // 3. 更新业务表关联
        purchase.setProcessInstanceId(processInstance.getId());
        purchaseMapper.updateById(purchase);
    }
    
    /**
     * 查询个人的采购单列表
     */
    public List<Purchase> listMyPurchases(String userId) {
        return purchaseMapper.selectList(
                new QueryWrapper<Purchase>().eq("user_id", userId).orderByDesc("create_time")
        );
    }

    /**
     * 由于审批统一在全局待办处理，此处仅提供状态更新支持
     * 一般建议将 approve/reject 写在公共的 TaskService 中，但为演示隔离放这里
     */
}
