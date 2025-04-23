package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

// 课程发布服务类
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Resource
    private CoursePublishPreMapper coursePublishPreMapper;;

    @Resource
    private CoursePublishMapper coursePublishMapper;

    @Resource
    private CourseBaseInfoService courseBaseInfoService;

    @Resource
    private TeachplanService teachplanService;

    @Resource
    private CourseMarketMapper courseMarketMapper;

    @Resource
    private CourseBaseMapper courseBaseMapper;

    @Resource
    private MqMessageService mqMessageService;

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId){
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        // 课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        // 课程计划信息
        List<TeachplanDto> teachPlanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachPlanTree);
        // 教师信息

        return coursePreviewDto;
    }

    /**
     * 提交课程审核
     * @param courseId 课程id
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId){
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfoDto == null){
            XueChengPlusException.cast("课程不存在");
        }

        // 审核状态
        String auditStatus = courseBaseInfoDto.getAuditStatus();
        // 如果课程的审核状态为已提交则不允许提交
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("课程已提交，请等待审核");
        }

        // 本机构只能提交本机构的课程
        Long courseBaseInfoDtoCompanyId = courseBaseInfoDto.getCompanyId();
        if(!companyId.equals(courseBaseInfoDtoCompanyId)){
            XueChengPlusException.cast("本机构只能提交本机构的课程");
        }

        // 课程图片、计划信息没有填写也不允许提交
        String pic = courseBaseInfoDto.getPic();
        if(StringUtil.isEmpty(pic)){
            XueChengPlusException.cast("请上传课程图片");
        }
        // 查询课程计划
        List<TeachplanDto> teachPlanTree = teachplanService.findTeachplanTree(courseId);
        if(teachPlanTree == null || teachPlanTree.isEmpty()){
            XueChengPlusException.cast("请填写课程计划");
        }

        // 查询课程基本信息、营销信息、计划等信息插入到课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfoDto,coursePublishPre);
        // 设置机构id
        coursePublishPre.setCompanyId(companyId);
        // 营销信息
        CourseMarket  courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        // 计划信息
        String teachPlanTreeJson = JSON.toJSONString(teachPlanTree);
        coursePublishPre.setTeachplan(teachPlanTreeJson);
        // 更新状态为已提交
        coursePublishPre.setStatus("202003");
        // 提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        // 查询预发布表，如果有记录则更新，没有则插入
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreUpdate == null){
            // 插入
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            // 更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        // 更新课程基本信息表的审核状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");// 更新为已提交

        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId){
        // 查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XueChengPlusException.cast("课程没有审核记录，无法发布");
        }
        // 状态
        String status = coursePublishPre.getStatus();
        // 课程如果没有审核通过不允许发布
        if(!"202004".equals(status)){
            XueChengPlusException.cast("课程没有审核通过，不允许发布");
        }
        // 向发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        // 查询课程发布，如果有则更新，没有则插入
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if(coursePublishUpdate == null){
            // 插入
            coursePublishMapper.insert(coursePublish);
        }else{
            // 更新
            coursePublishMapper.updateById(coursePublish);
        }
        // 向消息表写入数据
        saveCoursePublishMessage(courseId);
        // 将预发布表数据删除
        int result = coursePublishPreMapper.deleteById(courseId);
        if(result < 1){
            XueChengPlusException.cast("删除课程预发布表记录失败");
        }
    }

    /**
     * 保存消息表记录
     * @param courseId 课程id
     */
    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage == null){
            XueChengPlusException.cast(CommonError.UNKNOWN_ERROR);
        }
    }
}
