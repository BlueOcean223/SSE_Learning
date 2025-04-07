package com.xuecheng.content.service;


import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

// 课程教师信息管理接口
public interface CourseTeacherInfoService {
    /**
     * 查询教师列表
     * @param courseId 课程id
     * @return 教师列表
     */
    List<CourseTeacher> queryTeacherList(Long courseId);

    /**
     * 添加或修改教师
     * @param courseTeacher
     * @return
     */
    CourseTeacher addTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师
     * @param courseId
     * @param id
     */
    void deleteTeacher(Long courseId, Long id);
}
