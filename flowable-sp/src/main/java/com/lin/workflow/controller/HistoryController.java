package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.service.WorkflowHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 历史记录接口
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private WorkflowHistoryService historyService;

    /**
     * 查询已完成的流程实例
     */
    @GetMapping("/process-instances")
    public Result<List<Map<String, Object>>> finishedInstances() {
        return Result.ok(historyService.listFinishedProcessInstances());
    }

    /**
     * 查询所有流程实例（运行中+已完成）
     */
    @GetMapping("/process-instances/all")
    public Result<List<Map<String, Object>>> allInstances() {
        return Result.ok(historyService.listAllProcessInstances());
    }

    /**
     * 查询指定用户已完成的任务
     */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> finishedTasks(@RequestParam String assignee) {
        return Result.ok(historyService.listFinishedTasks(assignee));
    }

    /**
     * 查询流程实例的审批轨迹
     */
    @GetMapping("/process-instances/{processInstanceId}/activities")
    public Result<List<Map<String, Object>>> activities(
            @PathVariable String processInstanceId) {
        return Result.ok(historyService.getProcessActivities(processInstanceId));
    }
}
