package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.entity.Leave;
import com.lin.workflow.service.LeaveService;
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
 * 请假业务接口 — 企业级（候选组 + 驳回退回版）
 */
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    // ==================== 申请人接口 ====================

    /**
     * 提交请假申请
     */
    @PostMapping
    public Result<Leave> submit(@RequestBody Leave leave) {
        if (leave.getUserId() == null || leave.getUserId().isEmpty()) {
            return Result.fail("申请人不能为空");
        }
        if (leave.getReason() == null || leave.getReason().isEmpty()) {
            return Result.fail("请假事由不能为空");
        }
        return Result.ok(leaveService.submitLeave(leave));
    }

    /**
     * 查询我的请假单列表
     */
    @GetMapping
    public Result<List<Leave>> list(@RequestParam String userId) {
        return Result.ok(leaveService.listByUserId(userId));
    }

    /**
     * 查询所有请假单
     */
    @GetMapping("/all")
    public Result<List<Leave>> listAll() {
        return Result.ok(leaveService.listAll());
    }

    /**
     * 请假单详情
     */
    @GetMapping("/{id}")
    public Result<Leave> detail(@PathVariable Long id) {
        Leave leave = leaveService.getById(id);
        if (leave == null) return Result.fail("请假单不存在");
        return Result.ok(leave);
    }

    /**
     * 撤销请假申请（强制终止流程）
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return Result.ok();
    }

    // ==================== 审批人接口 ====================

    /**
     * 查询待办任务（包含候选组待认领 + 已认领的个人任务）
     */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks(@RequestParam String userId) {
        return Result.ok(leaveService.getTasksForUser(userId));
    }

    /**
     * 认领任务（候选组 → 个人）
     */
    @PostMapping("/tasks/{taskId}/claim")
    public Result<Void> claim(@PathVariable String taskId,
                              @RequestParam String userId) {
        leaveService.claimTask(taskId, userId);
        return Result.ok();
    }

    /**
     * 取消认领（退回候选组池）
     */
    @PostMapping("/tasks/{taskId}/unclaim")
    public Result<Void> unclaim(@PathVariable String taskId) {
        leaveService.unclaimTask(taskId);
        return Result.ok();
    }

    /**
     * 审批通过
     */
    @PostMapping("/tasks/{taskId}/approve")
    public Result<Void> approve(@PathVariable String taskId,
                                @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        leaveService.approve(taskId, comment);
        return Result.ok();
    }

    /**
     * 审批驳回（退回给申请人修改）
     */
    @PostMapping("/tasks/{taskId}/reject")
    public Result<Void> reject(@PathVariable String taskId,
                               @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        leaveService.reject(taskId, comment);
        return Result.ok();
    }

    /**
     * 重新提交（申请人修改后）
     */
    @PostMapping("/tasks/{taskId}/resubmit")
    public Result<Void> resubmit(@PathVariable String taskId,
                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        leaveService.resubmit(taskId, reason);
        return Result.ok();
    }

    /**
     * 撤回申请（在修改节点放弃提交）
     */
    @PostMapping("/tasks/{taskId}/withdraw")
    public Result<Void> withdraw(@PathVariable String taskId) {
        leaveService.withdraw(taskId);
        return Result.ok();
    }
}
