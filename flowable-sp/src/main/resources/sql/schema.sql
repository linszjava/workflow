-- 请假单业务表
CREATE TABLE IF NOT EXISTS biz_leave (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id             VARCHAR(32)   NOT NULL COMMENT '申请人ID',
    leave_type          VARCHAR(16)   NOT NULL COMMENT '请假类型: annual-年假 sick-病假 personal-事假',
    start_date          DATE          NOT NULL COMMENT '开始日期',
    end_date            DATE          NOT NULL COMMENT '结束日期',
    days                INT           NOT NULL COMMENT '请假天数',
    reason              VARCHAR(500)  NOT NULL COMMENT '请假事由',
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销',
    process_instance_id VARCHAR(64)   DEFAULT NULL COMMENT '关联的流程实例ID',
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假单业务表';
