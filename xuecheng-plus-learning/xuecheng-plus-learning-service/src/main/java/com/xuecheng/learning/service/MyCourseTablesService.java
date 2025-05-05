package com.xuecheng.learning.service;


import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

/**
 * 选课相关接口
 */
public interface MyCourseTablesService {

    /**
     * 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     */
    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 判断学习资格
     * @param userId 用户id
     * @param courseId 课程id
     */
    XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * 保存选课成功状态
     */
    boolean saveChooseCourseSuccess(String chooseCourseId);

    /**
     * 我的课程表
     * @param params 我的课程表查询参数
     * @return 分页参数
     */
    PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params);
}
