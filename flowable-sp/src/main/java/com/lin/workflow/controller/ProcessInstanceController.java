package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.service.WorkflowInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例管理接口
 */
@RestController
@RequestMapping("/api/process-instances")
public class ProcessInstanceController {

    @Autowired
    private WorkflowInstanceService instanceService;

    /**
     * 发起新的流程实例
     * 请求体示例：
     * {
     *   "processDefinitionKey": "leaveProcess",
     *   "businessKey": "LEAVE-2024-001",
     *   "initiator": "zhangsan",
     *   "variables": { "reason": "年假", "days": 3 }
     * }
     */
    @PostMapping
    public Result<Map<String, Object>> start(@RequestBody Map<String, Object> request) {
        String processDefinitionKey = (String) request.get("processDefinitionKey");
        String businessKey = (String) request.get("businessKey");
        String initiator = (String) request.get("initiator");

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");

        if (processDefinitionKey == null || processDefinitionKey.isEmpty()) {
            return Result.fail("processDefinitionKey 不能为空");
        }
        if (initiator == null || initiator.isEmpty()) {
            return Result.fail("initiator（发起人）不能为空");
        }

        Map<String, Object> result = instanceService.startProcess(
                processDefinitionKey, businessKey, initiator, variables);
        return Result.ok(result);
    }

    /**
     * 查询运行中的流程实例列表
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(instanceService.listRunningInstances());
    }

    /**
     * 终止流程实例
     */
    @DeleteMapping("/{instanceId}")
    public Result<Void> delete(@PathVariable String instanceId,
                               @RequestParam(required = false) String reason) {
        instanceService.deleteProcessInstance(instanceId, reason);
        return Result.ok();
    }
}
