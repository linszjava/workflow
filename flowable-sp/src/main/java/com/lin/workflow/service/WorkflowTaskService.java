package com.lin.workflow.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理服务
 */
@Service
public class WorkflowTaskService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    /**
     * 查询指定用户的待办任务
     */
    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("taskName", task.getName());
            map.put("taskDefinitionKey", task.getTaskDefinitionKey());
            map.put("assignee", task.getAssignee());
            map.put("createTime", task.getCreateTime());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("processDefinitionId", task.getProcessDefinitionId());

            // 获取流程变量（发起人等信息）
            try {
                Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());
                map.put("initiator", vars.get("initiator"));
                map.put("reason", vars.get("reason"));
                map.put("days", vars.get("days"));
            } catch (Exception e) {
                // 流程实例可能已结束
            }

            result.add(map);
        }
        return result;
    }

    /**
     * 获取任务详情
     */
    public Map<String, Object> getTaskDetail(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("taskId", task.getId());
        map.put("taskName", task.getName());
        map.put("taskDefinitionKey", task.getTaskDefinitionKey());
        map.put("assignee", task.getAssignee());
        map.put("createTime", task.getCreateTime());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("processDefinitionId", task.getProcessDefinitionId());
        map.put("description", task.getDescription());

        // 获取流程变量
        try {
            Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());
            map.put("variables", vars);
        } catch (Exception e) {
            // 忽略
        }

        return map;
    }

    /**
     * 完成（审批）任务
     *
     * @param taskId   任务 ID
     * @param approved 是否通过
     * @param comment  审批意见
     */
    public void completeTask(String taskId, boolean approved, String comment) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已被处理: " + taskId);
        }

        // 添加审批意见
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        // 设置审批结果变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);

        taskService.complete(taskId, variables);
    }
}
