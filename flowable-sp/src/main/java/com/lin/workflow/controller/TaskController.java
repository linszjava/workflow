package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.service.WorkflowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private WorkflowTaskService taskService;

    @GetMapping
    public Result<List<Map<String, Object>>> getTasks(@RequestParam String userId) {
        return Result.ok(taskService.getTasksForUser(userId));
    }

    @PostMapping("/{taskId}/approve")
    public Result<String> approve(@PathVariable String taskId, @RequestBody Map<String, String> params) {
        taskService.approveTask(taskId, params.get("comment"));
        return Result.ok("审批通过");
    }

    @PostMapping("/{taskId}/reject")
    public Result<String> reject(@PathVariable String taskId, @RequestBody Map<String, String> params) {
        taskService.rejectTask(taskId, params.get("comment"));
        return Result.ok("审批已驳回");
    }

    @PostMapping("/{taskId}/claim")
    public Result<String> claim(@PathVariable String taskId, @RequestBody Map<String, String> params) {
        taskService.claimTask(taskId, params.get("userId"));
        return Result.ok("认领成功");
    }

    @PostMapping("/{taskId}/unclaim")
    public Result<String> unclaim(@PathVariable String taskId) {
        taskService.unclaimTask(taskId);
        return Result.ok("取消认领成功");
    }

    @PostMapping("/{taskId}/transfer")
    public Result<String> transfer(@PathVariable String taskId, @RequestBody Map<String, String> params) {
        taskService.transferTask(taskId, params.get("targetUserId"));
        return Result.ok("任务已永久转交他人");
    }

    @PostMapping("/{taskId}/delegate")
    public Result<String> delegateTask(@PathVariable String taskId, @RequestBody Map<String, String> params) {
        taskService.delegateTask(taskId, params.get("targetUserId"));
        return Result.ok("任务已委派，完成后将返回给您");
    }
}
