package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

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

    /**
     * 课程静态化
     * @param courseId 课程id
     * @return File 静态化文件
     */
    File generateCourseHtml(Long courseId);

    /**
     * 上传课程静态化页面
     * @param file 静态化文件
     */
    void uploadCourseHtml(File file, Long courseId);

    /**
     * 根据id查询课程发布消息
     * @param courseId 课程id
     * @return
     */
    CoursePublish getCoursePubliah(Long courseId);
}
