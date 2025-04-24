package com.xuecheng.content.service.jobhandler;


import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

// 课程发布的任务类
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Resource
    private CoursePublishService coursePublishService;

    @Resource
    private CoursePublishMapper coursePublishMapper;

    @Resource
    private SearchServiceClient searchServiceClient;

    // 任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        // 调用抽象类的方法执行任务
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }


    // 课程发布任务处理
    @Override
    public boolean execute(MqMessage mqMessage){
        // 获取消息相关的业务信息
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        // 课程静态化,上传到minio
        generateCourseHtml(mqMessage, courseId);
        // 课程索引，向es写索引
        saveCourseIndex(mqMessage, courseId);
        // 课程缓存，向redis写缓存
        saveCourseCache(mqMessage, courseId);
        // 向消息表写入记录，任务完成
        return true;
    }

    // 生成课程静态页面
    private void generateCourseHtml(MqMessage mqMessage,Long courseId){
        // 消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        // 任务幂等性处理
        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne > 0){
            log.debug("课程静态化已完成，无需处理");
            return;
        }
        // 课程静态化
        // 生成html页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file == null){
            XueChengPlusException.cast("生成静态页面为空");
        }
        // 上传到minio
        coursePublishService.uploadCourseHtml(file,courseId);
        // 阶段任务处理完成
        mqMessageService.completedStageOne(taskId);
    }

    // 保存课程索引
    private void saveCourseIndex(MqMessage mqMessage,Long courseId){
        // 任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 幂等性处理
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo > 0){
            log.debug("课程索引已写入，无需处理");
            return;
        }
        // 添加索引
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);

        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);

        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("远程调用搜索服务添加索引失败");
        }

        // 完成阶段任务
        mqMessageService.completedStageTwo(taskId);
    }

    // 保存课程缓存
    private void saveCourseCache(MqMessage mqMessage,Long courseId){
        // 任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 幂等性处理
        int stageThree = mqMessageService.getStageThree(taskId);
        if(stageThree > 0){
            log.debug("课程缓存已写入，无需处理");
            return;
        }
        // 写入缓存

        // 完成阶段任务
        mqMessageService.completedStageThree(taskId);
    }
}
