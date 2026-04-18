package com.lin.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.workflow.entity.Leave;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假单 Mapper
 */
@Mapper
public interface LeaveMapper extends BaseMapper<Leave> {
}
