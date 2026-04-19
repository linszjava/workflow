package com.lin.workflow.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 自动催办服务器
 * 当挂载在节点上的 Timer 到期时执行，且非中断(cancelActivity=false)
 */
@Component("urgeService")
public class UrgeService implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        String initiator = (String) execution.getVariable("initiator");
        System.out.println("=========================================");
        System.out.println("⏰ 【系统警告】：触发自动超时机器引擎");
        System.out.println("📧 叮！正在向 " + initiator + " 的上级发送催办夺命连环邮件...");
        System.out.println("=========================================");
    }
}
