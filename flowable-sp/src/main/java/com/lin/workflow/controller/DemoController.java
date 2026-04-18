package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证 Flowable 引擎装配是否成功的测试接口
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    // 只需要用到这三个核心的服务类，Flowable 的精髓都在这里。
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    /**
     * 访问 http://localhost:8081/api/demo/status 测试连通性
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = new HashMap<>();
        
        // 查询目前系统里一共部署了多少个流程定义 (XML 文件)
        long processDefinitionCount = repositoryService.
                createProcessDefinitionQuery().count();
        // 查询目前系统里正在运行（执行中）的流程实例有多少个
        long processInstanceCount = runtimeService.
                createProcessInstanceQuery().count();
        // 查询目前系统里未完成的人工任务（Task）有多少个
        long taskCount = taskService.createTaskQuery().count();

        result.put("status", "UP");
        result.put("flowableVersion", "7.2.0");
        result.put("totalProcessDefinitions", processDefinitionCount);
        result.put("runningProcessInstances", processInstanceCount);
        result.put("pendingTasks", taskCount);

        return Result.ok(result);
    }
}
