package com.xuecheng.learning.service;


import com.xuecheng.base.model.RestResponse;

/**
 * 在线学习相关接口
 */
public interface LearningService {

    /**
     * 获取教学视频
     * @param userId 用户Id
     * @param courseId 课程Id
     * @param teachPlanId 课程计划Id
     * @param mediaId 视频Id
     * @return
     */
    RestResponse<String> getVideo(String userId, Long courseId, Long teachPlanId, String mediaId);
}
