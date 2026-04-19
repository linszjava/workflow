package com.lin.workflow.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lin.workflow.entity.BizUser;
import com.lin.workflow.mapper.BizUserMapper;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化 Flowable 用户和候选组
 *
 * 企业项目中这些数据通常从 LDAP / 组织架构表同步，
 * 这里用代码模拟，方便学习候选组机制。
 *
 * 用户 → 候选组 映射：
 *   zhangsan  → (员工，无审批候选组)
 *   manager   → deptManager（部门经理组）
 *   manager2  → deptManager（部门经理组，同组第二人，演示竞争认领）
 *   hr        → hrGroup（HR组）
 */
@Component
public class FlowableIdentityConfig implements CommandLineRunner {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private BizUserMapper bizUserMapper;
    @Override
    public void run(String... args) {
        // 创建用户及落库组织架构表 (设置明确的汇报线 managerId)
        createUserIfNotExists("zhangsan", "张三", "员工", "manager");
        createUserIfNotExists("lisi", "李四", "员工", "manager2");
        
        createUserIfNotExists("manager", "李经理", "部门经理", null);
        createUserIfNotExists("manager2", "赵副经理", "副经理", "manager");
        createUserIfNotExists("hr", "王HR", "人力资源", null);
        
        // 采购流程节点用户
        createUserIfNotExists("finance", "钱财务", "财务部", null);
        createUserIfNotExists("expert1", "周专家", "外聘专家", null);
        createUserIfNotExists("expert2", "吴专家", "外聘专家", null);
        createUserIfNotExists("expert3", "郑专家", "外聘专家", null);
        // 创建候选组
        createGroupIfNotExists("deptManager", "部门经理组");
        createGroupIfNotExists("hrGroup", "HR组");

        // 用户加入候选组
        addMembershipIfNotExists("manager", "deptManager");
        addMembershipIfNotExists("manager2", "deptManager");
        addMembershipIfNotExists("hr", "hrGroup");

        System.out.println("[Flowable] 用户和候选组初始化完成:");
        System.out.println("  deptManager 组: manager, manager2");
        System.out.println("  hrGroup 组: hr");
    }

    private void createUserIfNotExists(String userId, String firstName, String lastName, String managerId) {
        if (identityService.createUserQuery().userId(userId).count() == 0) {
            User user = identityService.newUser(userId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            identityService.saveUser(user);
        }
        
        // 双写同步至组织架构业务表 biz_user
        long count = bizUserMapper.selectCount(new LambdaQueryWrapper<BizUser>().eq(BizUser::getUserId, userId));
        if (count == 0) {
            BizUser bizUser = new BizUser();
            bizUser.setUserId(userId);
            bizUser.setUserName(firstName + lastName); // 业务表合成为全名
            bizUser.setRoleName(lastName);
            bizUser.setManagerId(managerId);
            bizUserMapper.insert(bizUser);
        }
    }

    private void createGroupIfNotExists(String groupId, String groupName) {
        if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
            Group group = identityService.newGroup(groupId);
            group.setName(groupName);
            identityService.saveGroup(group);
        }
    }

    private void addMembershipIfNotExists(String userId, String groupId) {
        // 先检查是否已经在此组中，避免触发主键冲突导致事务标记为 rollback-only
        long count = identityService.createUserQuery()
                .userId(userId)
                .memberOfGroup(groupId)
                .count();
        if (count == 0) {
            identityService.createMembership(userId, groupId);
        }
    }
}
