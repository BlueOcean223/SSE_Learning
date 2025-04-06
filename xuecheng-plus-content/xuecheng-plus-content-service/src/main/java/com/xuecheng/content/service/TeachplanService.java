package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.SavaTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

// 课程计划管理相关接口
public interface TeachplanService {
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程计划
     * @return 课程计划信息
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto 课程计划信息
     */
    public void saveTeachplan(SavaTeachplanDto saveTeachplanDto);
}
