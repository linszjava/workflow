package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.service.WorkflowDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 流程定义管理接口
 */
@RestController
@RequestMapping("/api/process-definitions")
public class ProcessDefinitionController {

    @Autowired
    private WorkflowDefinitionService definitionService;

    /**
     * 查询所有流程定义
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(definitionService.listProcessDefinitions());
    }

    /**
     * 获取流程定义 XML 内容
     */
    @GetMapping("/{deploymentId}/xml")
    public Result<String> getXml(@PathVariable String deploymentId) {
        String xml = definitionService.getProcessDefinitionXml(deploymentId);
        if (xml == null) {
            return Result.fail("流程定义不存在");
        }
        return Result.ok(xml);
    }

    /**
     * 删除部署（级联删除）
     */
    @DeleteMapping("/{deploymentId}")
    public Result<Void> delete(@PathVariable String deploymentId) {
        definitionService.deleteDeployment(deploymentId);
        return Result.ok();
    }
}
