package com.lin.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lin.workflow.common.Result;
import com.lin.workflow.entity.BizUser;
import com.lin.workflow.mapper.BizUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户/组织架构 API 接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private BizUserMapper bizUserMapper;

    /**
     * 获取系统中所有员工，供前端在转交委派时作为下拉项
     */
    @GetMapping("/list")
    public Result<List<BizUser>> listAllUsers() {
        List<BizUser> list = bizUserMapper.selectList(new LambdaQueryWrapper<>());
        return Result.ok(list);
    }
}
