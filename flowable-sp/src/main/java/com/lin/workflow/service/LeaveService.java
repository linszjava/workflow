package com.lin.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lin.workflow.entity.Leave;
import com.lin.workflow.mapper.LeaveMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
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
 * 请假业务服务 — 候选组 + 驳回退回版
 *
 * 候选组核心流程：
 *   1. 任务分配给候选组（如 deptManager），组内所有人都能看到
 *   2. 某人「认领」任务后，其他人就看不到了
 *   3. 认领人完成审批
 *
 * 驳回退回流程：
 *   经理/HR 驳回 → 流转到「申请人修改」节点（assignee=${initiator}）
 *   申请人可以「重新提交」(resubmit=true) 或「撤回」(resubmit=false)
 */
@Service
public class LeaveService {

    private static final String PROCESS_KEY = "leaveProcess";

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private IdentityService identityService;

    // ==================== 申请人操作 ====================

    /**
     * 提交请假申请（保存业务表 + 发起流程）
     */
    @Transactional
    public Leave submitLeave(Leave leave) {
        leave.setStatus(1);
        leaveMapper.insert(leave);

        // 设置流程发起人（用于 ${initiator} 表达式）
        identityService.setAuthenticatedUserId(leave.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", leave.getUserId());
        variables.put("leaveType", leave.getLeaveType());
        variables.put("days", leave.getDays());
        variables.put("reason", leave.getReason());
        variables.put("bizId", String.valueOf(leave.getId()));

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                String.valueOf(leave.getId()),
                variables
        );

        leave.setProcessInstanceId(instance.getId());
        leaveMapper.updateById(leave);

        return leave;
    }

    /**
     * 重新提交（申请人修改后）
     */
    @Transactional
    public void resubmit(String taskId, String reason) {
        Task task = getTaskOrThrow(taskId);

        // 更新业务表事由（如果修改了）
        Leave leave = getByProcessInstanceId(task.getProcessInstanceId());
        if (leave != null && reason != null && !reason.isEmpty()) {
            leave.setReason(reason);
            leave.setStatus(1); // 重新变为审批中
            leaveMapper.updateById(leave);
        }

        taskService.addComment(taskId, task.getProcessInstanceId(), "重新提交申请");

        Map<String, Object> variables = new HashMap<>();
        variables.put("resubmit", true);
        // 重新提交时清除 approved 变量，避免影响下次网关判断
        variables.put("approved", null);
        taskService.complete(taskId, variables);
    }

    /**
     * 撤回申请（在修改节点选择不再提交）
     */
    @Transactional
    public void withdraw(String taskId) {
        Task task = getTaskOrThrow(taskId);

        taskService.addComment(taskId, task.getProcessInstanceId(), "申请人撤回");

        Map<String, Object> variables = new HashMap<>();
        variables.put("resubmit", false);
        taskService.complete(taskId, variables);

        // 更新业务表状态
        updateLeaveStatus(task.getProcessInstanceId(), 4); // 已撤销
    }

    /**
     * 撤销请假申请（强制终止流程）
     */
    @Transactional
    public void cancel(Long leaveId) {
        Leave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw new RuntimeException("请假单不存在");
        }
        if (leave.getStatus() != 1) {
            throw new RuntimeException("只有审批中的请假单才能撤销");
        }
        if (leave.getProcessInstanceId() != null) {
            runtimeService.deleteProcessInstance(leave.getProcessInstanceId(), "申请人撤销");
        }
        leave.setStatus(4);
        leaveMapper.updateById(leave);
    }

    // ==================== 审批人操作 ====================

    /**
     * 认领任务（候选组 → 个人）
     * 候选组中的某个人认领后，该任务变为他的个人任务，其他人看不到了
     */
    public void claimTask(String taskId, String userId) {
        Task task = getTaskOrThrow(taskId);
        if (task.getAssignee() != null) {
            throw new RuntimeException("任务已被 " + task.getAssignee() + " 认领");
        }
        taskService.claim(taskId, userId);
    }

    /**
     * 取消认领（退回到候选组池中）
     */
    public void unclaimTask(String taskId) {
        taskService.unclaim(taskId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(String taskId, String comment) {
        Task task = getTaskOrThrow(taskId);

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        taskService.complete(taskId, variables);

        // 流程结束时更新业务表
        updateLeaveStatusIfFinished(task.getProcessInstanceId(), 2);
    }

    /**
     * 审批驳回（退回给申请人修改，不再直接结束）
     */
    @Transactional
    public void reject(String taskId, String comment) {
        Task task = getTaskOrThrow(taskId);

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        taskService.complete(taskId, variables);

        // 驳回后业务表状态改为"已驳回"（申请人可以修改后重新提交）
        updateLeaveStatus(task.getProcessInstanceId(), 3);
    }

    // ==================== 查询 ====================

    /**
     * 查询待办任务（包含候选组的待认领任务 + 已认领的个人任务）
     */
    public List<Map<String, Object>> getTasksForUser(String userId) {
        // 1. 已认领给我的任务（assigned to me）
        List<Task> assignedTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .list();

        // 2. 我所在候选组的待认领任务（candidate）
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
        map.put("taskStatus", taskStatus); // assigned=已认领, candidate=待认领
        map.put("createTime", task.getCreateTime());
        map.put("processInstanceId", task.getProcessInstanceId());

        // 补充业务数据
        Leave leave = getByProcessInstanceId(task.getProcessInstanceId());
        if (leave != null) {
            map.put("leaveId", String.valueOf(leave.getId()));
            map.put("applicant", leave.getUserId());
            map.put("leaveType", leave.getLeaveType());
            map.put("startDate", leave.getStartDate());
            map.put("endDate", leave.getEndDate());
            map.put("days", leave.getDays());
            map.put("reason", leave.getReason());
        }

        // 补充流程变量
        Map<String, Object> variables = taskService.getVariables(task.getId());
        map.put("initiator", variables.get("initiator"));

        return map;
    }

    public List<Leave> listByUserId(String userId) {
        return leaveMapper.selectList(
                new LambdaQueryWrapper<Leave>()
                        .eq(Leave::getUserId, userId)
                        .orderByDesc(Leave::getCreateTime)
        );
    }

    public List<Leave> listAll() {
        return leaveMapper.selectList(
                new LambdaQueryWrapper<Leave>()
                        .orderByDesc(Leave::getCreateTime)
        );
    }

    public Leave getById(Long id) {
        return leaveMapper.selectById(id);
    }

    public Leave getByProcessInstanceId(String processInstanceId) {
        return leaveMapper.selectOne(
                new LambdaQueryWrapper<Leave>()
                        .eq(Leave::getProcessInstanceId, processInstanceId)
        );
    }

    // ==================== 内部方法 ====================

    private Task getTaskOrThrow(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已被处理");
        }
        return task;
    }

    private void updateLeaveStatusIfFinished(String processInstanceId, int targetStatus) {
        long count = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count();
        if (count == 0) {
            updateLeaveStatus(processInstanceId, targetStatus);
        }
    }

    private void updateLeaveStatus(String processInstanceId, int status) {
        Leave leave = getByProcessInstanceId(processInstanceId);
        if (leave != null) {
            leave.setStatus(status);
            leaveMapper.updateById(leave);
        }
    }
}
