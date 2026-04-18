package com.lin.workflow.service;

import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史记录服务
 */
@Service
public class WorkflowHistoryService {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    /**
     * 查询已完成的流程实例
     */
    public List<Map<String, Object>> listFinishedProcessInstances() {
        List<HistoricProcessInstance> instances = historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricProcessInstance inst : instances) {
            Map<String, Object> map = new HashMap<>();
            map.put("processInstanceId", inst.getId());
            map.put("processDefinitionId", inst.getProcessDefinitionId());
            map.put("processDefinitionKey", inst.getProcessDefinitionKey());
            map.put("processDefinitionName", inst.getProcessDefinitionName());
            map.put("businessKey", inst.getBusinessKey());
            map.put("startTime", inst.getStartTime());
            map.put("endTime", inst.getEndTime());
            map.put("durationInMillis", inst.getDurationInMillis());
            map.put("startUserId", inst.getStartUserId());

            // 获取历史变量中的发起人
            Map<String, Object> vars = getHistoricVariables(inst.getId());
            map.put("initiator", vars.get("initiator"));

            result.add(map);
        }
        return result;
    }

    /**
     * 查询所有流程实例（包括运行中和已完成）
     */
    public List<Map<String, Object>> listAllProcessInstances() {
        List<HistoricProcessInstance> instances = historyService
                .createHistoricProcessInstanceQuery()
                .orderByProcessInstanceStartTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricProcessInstance inst : instances) {
            Map<String, Object> map = new HashMap<>();
            map.put("processInstanceId", inst.getId());
            map.put("processDefinitionId", inst.getProcessDefinitionId());
            map.put("processDefinitionKey", inst.getProcessDefinitionKey());
            map.put("processDefinitionName", inst.getProcessDefinitionName());
            map.put("businessKey", inst.getBusinessKey());
            map.put("startTime", inst.getStartTime());
            map.put("endTime", inst.getEndTime());
            map.put("durationInMillis", inst.getDurationInMillis());
            map.put("finished", inst.getEndTime() != null);

            Map<String, Object> vars = getHistoricVariables(inst.getId());
            map.put("initiator", vars.get("initiator"));

            result.add(map);
        }
        return result;
    }

    /**
     * 查询指定用户已完成的任务
     */
    public List<Map<String, Object>> listFinishedTasks(String assignee) {
        List<HistoricTaskInstance> tasks = historyService
                .createHistoricTaskInstanceQuery()
                .taskAssignee(assignee)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("taskName", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("startTime", task.getCreateTime());
            map.put("endTime", task.getEndTime());
            map.put("durationInMillis", task.getDurationInMillis());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("processDefinitionId", task.getProcessDefinitionId());

            // 获取审批意见
            List<org.flowable.engine.task.Comment> comments = taskService
                    .getTaskComments(task.getId());
            if (!comments.isEmpty()) {
                map.put("comment", comments.get(comments.size() - 1).getFullMessage());
            }

            result.add(map);
        }
        return result;
    }

    /**
     * 查询流程实例的审批轨迹（活动列表）
     */
    public List<Map<String, Object>> getProcessActivities(String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricActivityInstance activity : activities) {
            Map<String, Object> map = new HashMap<>();
            map.put("activityId", activity.getActivityId());
            map.put("activityName", activity.getActivityName());
            map.put("activityType", activity.getActivityType());
            map.put("assignee", activity.getAssignee());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            map.put("durationInMillis", activity.getDurationInMillis());
            map.put("taskId", activity.getTaskId());

            // 如果是用户任务，获取审批意见
            if ("userTask".equals(activity.getActivityType()) && activity.getTaskId() != null) {
                List<org.flowable.engine.task.Comment> comments = taskService
                        .getTaskComments(activity.getTaskId());
                if (!comments.isEmpty()) {
                    map.put("comment", comments.get(comments.size() - 1).getFullMessage());
                }
            }

            result.add(map);
        }
        return result;
    }

    /**
     * 获取历史流程变量
     */
    private Map<String, Object> getHistoricVariables(String processInstanceId) {
        Map<String, Object> vars = new HashMap<>();
        try {
            historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list()
                    .forEach(v -> vars.put(v.getVariableName(), v.getValue()));
        } catch (Exception e) {
            // 忽略
        }
        return vars;
    }
}
