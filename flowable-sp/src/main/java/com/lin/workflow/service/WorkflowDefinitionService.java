package com.lin.workflow.service;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义管理服务
 */
@Service
public class WorkflowDefinitionService {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 查询所有流程定义
     */
    public List<Map<String, Object>> listProcessDefinitions() {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessDefinition def : definitions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", def.getId());
            map.put("key", def.getKey());
            map.put("name", def.getName());
            map.put("version", def.getVersion());
            map.put("deploymentId", def.getDeploymentId());
            map.put("suspended", def.isSuspended());
            map.put("category", def.getCategory());

            // 获取部署时间
            Deployment deployment = repositoryService.createDeploymentQuery()
                    .deploymentId(def.getDeploymentId())
                    .singleResult();
            if (deployment != null) {
                map.put("deploymentTime", deployment.getDeploymentTime());
                map.put("deploymentName", deployment.getName());
            }
            result.add(map);
        }
        return result;
    }

    /**
     * 获取流程定义 XML
     */
    public String getProcessDefinitionXml(String deploymentId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        if (definition == null) {
            return null;
        }

        try (InputStream inputStream = repositoryService.getResourceAsStream(
                deploymentId, definition.getResourceName())) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取流程定义 XML 失败", e);
        }
    }

    /**
     * 删除部署（级联删除关联的流程实例）
     */
    public void deleteDeployment(String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
    }
}
