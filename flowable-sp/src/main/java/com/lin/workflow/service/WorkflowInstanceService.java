package com.lin.workflow.service;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例管理服务
 */
@Service
public class WorkflowInstanceService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    /**
     * 发起流程实例
     *
     * @param processDefinitionKey 流程定义 Key（如 leaveProcess）
     * @param businessKey          业务标识
     * @param initiator            发起人
     * @param variables            流程变量
     */
    public Map<String, Object> startProcess(String processDefinitionKey, String businessKey,
                                            String initiator, Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        // 将发起人存入流程变量
        variables.put("initiator", initiator);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey, businessKey, variables);

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("processDefinitionId", instance.getProcessDefinitionId());
        result.put("processDefinitionKey", instance.getProcessDefinitionKey());
        result.put("businessKey", instance.getBusinessKey());
        return result;
    }

    /**
     * 查询运行中的流程实例列表
     *
     * {processInstanceId: "155a4119-3af9-11f1-b1aa-6e312489af45",…}
     * businessKey
     * :
     * "BIZ-1776497690389"
     * currentTasks
     * :
     * ["部门经理审批"]
     * initiator
     * :
     * "zhangsan"
     * processDefinitionId
     * :
     * "leaveProcess:2:9b1dc097-3af6-11f1-be8f-5299301fe398"
     * processDefinitionKey
     * :
     * "leaveProcess"
     * processDefinitionName
     * :
     * "请假流程"
     * processInstanceId
     * :
     * "155a4119-3af9-11f1-b1aa-6e312489af45"
     * startTime
     * :
     * "2026-04-18T07:34:50.470+00:00"
     * startUserId
     * :
     * null
     *
     */
    public List<Map<String, Object>> listRunningInstances() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc() // 开始时间降序
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessInstance inst : instances) {
            Map<String, Object> map = new HashMap<>();
            map.put("processInstanceId", inst.getId());
            map.put("processDefinitionId", inst.getProcessDefinitionId());
            map.put("processDefinitionKey", inst.getProcessDefinitionKey());
            map.put("processDefinitionName", inst.getProcessDefinitionName());
            map.put("businessKey", inst.getBusinessKey());
            map.put("startTime", inst.getStartTime());
            map.put("startUserId", inst.getStartUserId());

            // 查询当前节点任务
            List<Task> currentTasks = taskService.createTaskQuery()
                    .processInstanceId(inst.getId())
                    .list();
            List<String> currentTaskNames = new ArrayList<>();
            for (Task t : currentTasks) {
                currentTaskNames.add(t.getName());
            }
            map.put("currentTasks", currentTaskNames);

            // 从流程变量获取发起人
            Map<String, Object> vars = runtimeService.getVariables(inst.getId());
            map.put("initiator", vars.get("initiator"));

            result.add(map);
        }
        return result;
    }

    /**
     * 终止流程实例
     */
    public void deleteProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId,
                reason != null ? reason : "管理员手动终止");
    }
}
