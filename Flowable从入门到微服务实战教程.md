# Flowable 从入门到微服务实战教程（零基础版）

欢迎来到 Flowable 的世界！工作流虽然看起来复杂，但它的本质其实就是一个**状态机**。学习 Flowable 不建议一上来就死磕到底层源码，最快的方法是：**理清概念 -> 画图感受 -> 基本 API 跑通 -> 结合实际业务 -> 微服务整合**。

这份教程专为你量身定制，我们将一步步拆解，带你从“零基础小白”成长为“工作流高手”。

---

## 阶段一：打破认知——搞懂工作流的“暗语”和“结构”

在写任何代码之前，你必须知道 Flowable 业务运作的三个要素：**画图、发版、运转**。

### 1. 什么是 BPMN 2.0？
Flowable 使用的是 BPMN 2.0 标准。你可以简单把它理解为：**一种特定格式的 XML 文件**。无论你在高大上的可视化界面里怎么拖拽连线，最终存入数据库的，就是一份定义了节点、连线和规则的 XML 文件。

### 2. 核心四大对象（务必死记硬背）
工作流的操作完全围绕以下四个不同生命周期的词汇展开：
1.  **Deployment（部署）**：就是“发版”。你把画好的 XML 上传给系统，这就叫一次部署。
2.  **ProcessDefinition（流程定义）**：代码里的类（Class）。例如：“请假流程V1.0”。
3.  **ProcessInstance（流程实例）**：代码里 new 出来的对象（Object）。例如：“张三正在发起的 8 月 12 日的请假”。
4.  **Task（任务）**：实例运转过程中停留下来的“节点”。例如：这个流程走到了“经理审批”节点，这就生成了一个 Task。

### 3. Flowable 的“五大神兽” Service
Flowable 在 Spring Boot 中提供了几个核心接口帮我们干活：
*   **`RepositoryService`**：仓库服务。用来管理（查询、删除）刚才说到的“流程定义”（ProcessDefinition）。
*   **`RuntimeService`**：运行服务。用来发起一个“流程实例”（ProcessInstance），以及查询正在运行中的流程数据。
*   **`TaskService`**：任务服务。和“人”最紧密的服务。用来查询某个人当前有哪些待办任务，以及做“同意”、“驳回”、“转办”等操作。
*   **`HistoryService`**：历史服务。一旦一个节点走完，运行表的数据就会被删除，留存到历史表。查以前办过的事、看流程记录都靠它。
*   **`ManagementService`**：管理服务。处理定时任务、死信队列等底层引擎配置（前期较少使用）。

---

## 阶段二：跑起你的第一个 Hello World（Spring Boot 整合）

别着急去啃微服务，先在一个干干净净的 Spring Boot 单体项目里跑通！

### 1. 引入依赖
如果是 Spring Boot 2.x 或 3.x，在你的 `pom.xml` 中引入专属 Starter：
```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-process</artifactId>
    <version>7.2.0</version> <!-- 请注意：如果是 JDK17+SpringBoot3，必须使用 7.x 版本 -->
</dependency>
<!-- 必须搭配一个数据库，例如 MySQL 驱动，Flowable 启动时会自动帮你建 30 多张表 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

### 2. 核心原理：数据库表自动生成
配置好 application.yml 里的数据库连接后，**直接启动 Spring Boot 项目**。
你会惊奇地发现，数据库里自动生成了大量以 `ACT_` 开头的表。主要分三类：
*   `ACT_RE_*`：(Repository) 存放静态流程定义和部署信息。
*   `ACT_RU_*`：(Runtime) 存放正在执行的实例、任务、变量。**节点一旦被审批，数据就会从这里消失。**
*   `ACT_HI_*`：(History) 历史表，记录所有走过的痕迹，用于留痕溯源。

### 3. 如何画图？
*   **最简体验**：在 IDEA 里安装全称为 **`Flowable BPMN visualizer`** 的插件。创建一个后缀名为 `.bpmn20.xml` 的文件，右键选择 `View BPMN Diagram` 即可以拖拽连线。
*   **官方标准**：下载官方的部署包运行 `flowable-ui`，用 Web 界面画图。（日常开发和调试推荐这种）

---

## 阶段三：掌握基本功 —— 核心 API 代码实战

假设我们在资源目录 `src/main/resources/processes/` 下放了一个名叫 `leave-process.bpmn20.xml` 的请假流程。

### 1. 发起请假流程（生成实例）
```java
@Autowired
private RuntimeService runtimeService;

public void startProcess() {
    // leaveProcess 是画图时你在图纸属性上填的 Process ID
    // businessKey 一般填你自己业务系统的请假单表的主键 ID（重要！用来关联业务表）
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("leaveProcess", "leave_order_1001");
    System.out.println("成功发起流程，实例ID：" + instance.getId());
}
```

### 2. 查询李四的待办任务
```java
@Autowired
private TaskService taskService;

public void getList() {
    List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee("李四") // 画图时绑定的审批人
            .list();
    for (Task task : tasks) {
         System.out.println("李四有待办任务：" + task.getName() + "，任务ID：" + task.getId());
    }
}
```

### 3. 李四点击“同意”
```java
public void completeTask(String taskId) {
    // 简单地把任务提交掉，进度就会流转到图上的下一根连线
    taskService.complete(taskId);
}
```

---

## 阶段四：进阶 —— 搞定中国式审批的灵魂机制

原生的 Flowable 不能满足真实的国内需求，你必须学会下面这些机制：

### 1. 流程变量 (Variables)
流程如何自动决定是走“部门经理”还是走“大老板”？必须靠连线上的“网关条件”（例如 `${days > 3}`）。
那么 `days` 从哪来？我们需要在发起流程或审批时传入**全局流程变量**：
```java
Map<String, Object> variables = new HashMap<>();
variables.put("days", 5);
taskService.complete(taskId, variables);
```

### 2. 多实例 (Multi-Instance) —— 会签与或签
老板安排了 3 个人进行跨部门审批：
*   **会签**：3 个人都要点同意，才能走下去。
*   **或签**：3 个人只要有 1 人点同意，就往下走。
* **做法**：在画图面板中，选中任务节点，将其设置配置为 Multi-Instance。我们通常传入一个 List 给引擎，List 里包含这 3 个人的名字，引擎会自动复制出 3 个并行的 Task。

### 3. 监听器 (Listener) —— 绝密武器
你的业务代码该在哪里写？怎么保证解耦？必须利用监听器！
*   **执行监听器 (Execution Listener)**：在连线的流动、节点的开启或结束时触发。
*   **任务监听器 (Task Listener)**：最常用！当一个审批人工单被创建 (Create)、指派 (Assignment)、完成 (Complete) 时触发任务。
* *实战用途*：很多公司用“任务完成监听器”，在节点通过时去修改自己业务表的状态（比如更新订单状态为已审核），或者发短信给下一个审批人。

---

## 阶段五：高阶 —— 微服务集成指南

恭喜你！学到这，单体已经难不倒你了。但在 Spring Cloud 等微服务里面使用，设计层面就会有挑战。微服务下集成 Flowable，通常有两种架构选择：

### 架构一：将 Flowable 作为一个独立服务（推荐大中型微服务）
做成一个专门的 `workflow-service` 服务，其他诸如 `order-service`, `hr-service` 都通过 OpenFeign 去调用它。
*   **好处**：流程引擎对内存占用相对较大，切分后不影响其他业务；业务清晰。
*   **坏处**：分布式事务！在 `order-service` 里请假表保存成功后，调用 `workflow-service` 发起流程失败，怎么办？
*   **对策**：如果项目采用这种模式，你现在学的 **Seata 分布式事务** 马上就能派上大用场。

### 架构二：每个微服务自行内嵌 Flowable（适合强隔离架构）
每个带有审批需求的微服务，POM 里自己引入 Flowable，拥有自己的引擎，连公用的数据库库（或分库）。
*   **好处**：没有分布式事务问题，直接本地事务包裹（`@Transactional`）`taskService.complete()` 和当前微服务的 `DB save()`。
*   **坏处**：数据库库表冗余或耦合，管理分散。国内较少采用这种做法。

### 微服务改造：废弃 Flowable 原生用户体系
Flowable 默认自带了名为 `ACT_ID_USER` / `ACT_ID_GROUP` 的组别表。
**在真实微服务中，绝对不要去使用它！** 你的微服务系统中肯定已经有了 `sys_user`、`sys_role`。
*   **正确做法**：画图时不要把 Assignee 写死成张三，而是写死你们微服务内用户的唯一身份标识（如工号或 Snowflake UUID ID）。
*   在前端查询待办时，先通过 Flowable 的 API 查出 `task` 里的 `assignee` 列表，再拿着这些 UUID 列表，去你的用户微服务 (`user-service`) 批量 `IN` 查询真实的姓名和头像返回给前端。

---

## 下一步建议

1.  **实操画图**：马上去 IDEA 下载 `Flowable BPMN visualizer` 插件，随便拖拽建立一个节点 A 到节点 B，理解 XML。
2.  **写个 Demo**：建一个空的 Spring Boot，通过上面的代码调起流程。
3.  **看一集视频**：在 B 站搜索“Flowable 黑马”或“Flowable 尚硅谷”，选择性只看**流程变量/网关/连线/监听器**这几个小节，看完立刻茅塞顿开。
4.  **对接项目**：尝试在你现在的 Seata 实战微服务体系中抽离出一个发请假单的案例，彻底吃透！
