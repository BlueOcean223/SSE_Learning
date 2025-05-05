package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service
public class LearningServiceImpl implements LearningService {


    @Resource
    private MyCourseTablesService myCourseTablesService;

    @Resource
    private ContentServiceClient contentServiceClient;

    @Resource
    private MediaServiceClient mediaServiceClient;


    /**
     * 获取教学视频
     * @param userId 用户Id
     * @param courseId 课程Id
     * @param teachPlanId 课程计划Id
     * @param mediaId 视频Id
     * @return
     */
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachPlanId, String mediaId){
        // 查询课程信息
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        if (coursePublish == null) {
            return RestResponse.validfail("课程不存在");
        }

        // 免费课程，可直接播放
        String charge = coursePublish.getCharge();
        if ("201000".equals(charge)) {
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        // 查询课程计划是否支持试学
        String teachPlan = coursePublish.getTeachplan();
        if (teachPlan == null) {
            return RestResponse.validfail("课程计划不存在");
        }
        List<TeachplanDto> teachPlanDtoList = JSON.parseArray(teachPlan, TeachplanDto.class);
        for (TeachplanDto teachplanDto : teachPlanDtoList){
            for(TeachplanDto child : teachplanDto.getTeachPlanTreeNodes()){
                if(child.getId().equals(teachPlanId) && child.getIsPreview().equals("1")){
                    return mediaServiceClient.getPlayUrlByMediaId(mediaId);
                }
            }
        }


        // 用户已登录，查看该用户是否购买了该课程且未过期
        if(StringUtils.isNotBlank(userId)){
            // 获取学习资格
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if("702002".equals(learnStatus)){
                return RestResponse.validfail("没有选课或选课后没有支付,无法学习");
            }else if("702003".equals(learnStatus)){
                return RestResponse.validfail("已过期,请重新支付");
            }else{
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }

        return RestResponse.validfail("课程需要购买");
    }
}
