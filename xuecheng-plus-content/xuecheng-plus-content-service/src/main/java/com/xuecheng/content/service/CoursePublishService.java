package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.CoursePreviewDto;

// 课程发布相关接口
public interface CoursePublishService {

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     */
    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 提交审核
     * @param courseId 课程id
     */
    void commitAudit(Long companyId,Long courseId);

    /**
     * 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void publish(Long companyId, Long courseId);
}
