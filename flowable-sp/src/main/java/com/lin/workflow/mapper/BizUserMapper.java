package com.lin.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.workflow.entity.BizUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务人员/组织架构 Mapper
 */
@Mapper
public interface BizUserMapper extends BaseMapper<BizUser> {
}
