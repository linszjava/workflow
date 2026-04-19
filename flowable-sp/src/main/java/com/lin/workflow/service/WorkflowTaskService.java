package com.lin.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lin.workflow.entity.Leave;
import com.lin.workflow.entity.Purchase;
import com.lin.workflow.mapper.LeaveMapper;
import com.lin.workflow.mapper.PurchaseMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一的待办任务服务
 */
@Service
public class WorkflowTaskService {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private PurchaseMapper purchaseMapper;

    /**
     * 查询全局统一待办（包含请假和采购等所有流程）
     */
    public List<Map<String, Object>> getTasksForUser(String userId) {
        List<Task> assignedTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .list();

        List<Task> candidateTasks = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskCreateTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : assignedTasks) {
            result.add(buildTaskMap(task, "assigned"));
        }
        for (Task task : candidateTasks) {
            result.add(buildTaskMap(task, "candidate"));
        }
        return result;
    }

    private Map<String, Object> buildTaskMap(Task task, String taskStatus) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", task.getId());
        map.put("taskName", task.getName());
        map.put("taskDefinitionKey", task.getTaskDefinitionKey());
        map.put("assignee", task.getAssignee());
        map.put("taskStatus", taskStatus);
        map.put("createTime", task.getCreateTime());
        map.put("processInstanceId", task.getProcessInstanceId());

        // 判断所属流程，拼装对应的业务数据
        String pdId = task.getProcessDefinitionId();
        map.put("processType", pdId.startsWith("leaveProcess") ? "leave" : (pdId.startsWith("purchaseProcess") ? "purchase" : "unknown"));

        if (pdId.startsWith("leaveProcess")) {
            Leave leave = leaveMapper.selectOne(new LambdaQueryWrapper<Leave>().eq(Leave::getProcessInstanceId, task.getProcessInstanceId()));
            if (leave != null) {
                map.put("bizId", String.valueOf(leave.getId()));
                map.put("applicant", leave.getUserId());
                map.put("bizTitle", "请假申请 - " + leave.getLeaveType());
                map.put("bizDetail", "请假 " + leave.getDays() + " 天，事由：" + leave.getReason());
            }
        } else if (pdId.startsWith("purchaseProcess")) {
            Purchase purchase = purchaseMapper.selectOne(new LambdaQueryWrapper<Purchase>().eq(Purchase::getProcessInstanceId, task.getProcessInstanceId()));
            if (purchase != null) {
                map.put("bizId", String.valueOf(purchase.getId()));
                map.put("applicant", purchase.getUserId());
                map.put("bizTitle", "采购申请 - " + purchase.getItemName());
                map.put("bizDetail", "数量：" + purchase.getQuantity() + " 单价：" + purchase.getPrice() + "，事由：" + purchase.getReason());
            }
        }

        Map<String, Object> variables = taskService.getVariables(task.getId());
        map.put("initiator", variables.get("initiator"));

        return map;
    }
    
    /**
     * 通用认领
     */
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    /**
     * 通用取消认领
     */
    public void unclaimTask(String taskId) {
        taskService.unclaim(taskId);
    }

    /**
     * 通用审批通过（兼容各种流程）
     */
    @Transactional
    public void approveTask(String taskId, String comment) {
        Task task = getTaskOrThrow(taskId);
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        taskService.complete(taskId, variables);

        // 会签检查处理：由于每个完成都会调用 complete，流程可能会继续。
        // 等流程实例自然结束时，自动把状态改为已通过，这里用统一的方法来尝试更新状态
        checkAndCompleteBusiness(task.getProcessInstanceId(), task.getProcessDefinitionId());
    }

    /**
     * 通用审批驳回
     */
    @Transactional
    public void rejectTask(String taskId, String comment) {
        Task task = getTaskOrThrow(taskId);
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        taskService.complete(taskId, variables);

        // 为了Demo简化，如果采购单有驳回逻辑，也在这更新。
        // 目前购买流程没有改退图，这里如果真驳回了，我们就改状态为3直接结束或等待。
        checkAndRejectBusiness(task.getProcessInstanceId(), task.getProcessDefinitionId());
    }

    private void checkAndCompleteBusiness(String processInstanceId, String pdId) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count();
        if (count == 0) { // 流程已结束
            if (pdId.startsWith("leaveProcess")) {
                Leave leave = leaveMapper.selectOne(new LambdaQueryWrapper<Leave>().eq(Leave::getProcessInstanceId, processInstanceId));
                if (leave != null) {
                    leave.setStatus(2);
                    leaveMapper.updateById(leave);
                }
            } else if (pdId.startsWith("purchaseProcess")) {
                Purchase purchase = purchaseMapper.selectOne(new LambdaQueryWrapper<Purchase>().eq(Purchase::getProcessInstanceId, processInstanceId));
                if (purchase != null) {
                    purchase.setStatus(2);
                    purchaseMapper.updateById(purchase);
                }
            }
        }
    }

    private void checkAndRejectBusiness(String processInstanceId, String pdId) {
        if (pdId.startsWith("purchaseProcess")) {
            Purchase purchase = purchaseMapper.selectOne(new LambdaQueryWrapper<Purchase>().eq(Purchase::getProcessInstanceId, processInstanceId));
            if (purchase != null) {
                purchase.setStatus(3);
                purchaseMapper.updateById(purchase);
            }
        }
    }

    private Task getTaskOrThrow(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已被处理");
        }
        return task;
    }
}
