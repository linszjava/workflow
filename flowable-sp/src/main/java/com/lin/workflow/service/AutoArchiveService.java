package com.lin.workflow.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 自动归档与通知系统服务
 * 当HR审批通过后，在完全结束前走这个纯机器自动节点
 */
@Component("autoArchiveService")
public class AutoArchiveService implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        String initiator = (String) execution.getVariable("initiator");
        System.out.println("=========================================");
        System.out.println("⚙️ 【机器处理节点】：HR 审批完毕，进入无人值守通道......");
        System.out.println("🤖 正在对接 [财务系统] 执行带薪扣减...");
        System.out.println("🤖 正在对接 [人力系统] 写入归档档案...");
        System.out.println("✅ " + initiator + " 的请假自动封卷完毕，流转至终点结束！");
        System.out.println("=========================================");
    }
}
