package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.service.WorkflowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 任务管理接口
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private WorkflowTaskService workflowTaskService;

    /**
     * 查询指定用户的待办任务
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam String assignee) {
        return Result.ok(workflowTaskService.getTasksByAssignee(assignee));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> detail(@PathVariable String taskId) {
        Map<String, Object> task = workflowTaskService.getTaskDetail(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        return Result.ok(task);
    }

    /**
     * 完成（审批）任务
     * 请求体示例：
     * {
     *   "approved": true,
     *   "comment": "同意请假"
     * }
     */
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable String taskId,
                                 @RequestBody Map<String, Object> request) {
        Boolean approved = (Boolean) request.get("approved");
        String comment = (String) request.get("comment");

        if (approved == null) {
            return Result.fail("approved 参数不能为空");
        }

        workflowTaskService.completeTask(taskId, approved, comment);
        return Result.ok();
    }
}
