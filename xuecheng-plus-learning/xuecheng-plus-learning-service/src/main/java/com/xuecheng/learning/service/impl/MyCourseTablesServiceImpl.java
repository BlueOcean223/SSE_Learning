package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Resource
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Resource
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Resource
    private ContentServiceClient contentServiceClient;


    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 调用内容管理查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish == null){
            XueChengPlusException.cast("课程不存在");
        }
        // 收费规则
        String charge = coursepublish.getCharge();
        // 选课记录
        XcChooseCourse xcChooseCourse = null;
        if("201000".equals(charge)){
            // 免费课程
            // 向选课记录表写数据
            xcChooseCourse = addFreeCourse(userId, coursepublish);
            // 向我的课程表写数据
            addCourseTables(xcChooseCourse);
        }else{
            // 收费课程
            // 向选课记录表写数据
            xcChooseCourse = addChargeCourse(userId, coursepublish);
        }

        // 判断学生的学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);

        // 构造返回值
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,xcChooseCourseDto);
        // 设置学习资格状态
        xcCourseTablesDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        // 返回结果
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();

        // 查询我的课程表，如果查不到说明没有选课
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        if(xcCourseTables == null){
            // 没有选课记录
            xcCourseTablesDto.setLearnStatus("702002");//未选课
            return xcCourseTablesDto;
        }

        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);

        // 如果查到了，判断是否过期
        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(before){
            // 已过期
            xcCourseTablesDto.setLearnStatus("702002");
        }else{
            // 未过期，正常学习
            xcCourseTablesDto.setLearnStatus("702001");
        }
        return xcCourseTablesDto;
    }


    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        //课程id
        Long courseId = coursepublish.getId();
        //判断，如果存在免费的选课记录且选课状态为成功，直接返回了
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if(!xcChooseCourses.isEmpty()){
            return xcChooseCourses.get(0);
        }

        //向选课记录表写数据
        XcChooseCourse chooseCourse = new XcChooseCourse();

        chooseCourse.setCourseId(courseId);
        chooseCourse.setCourseName(coursepublish.getName());
        chooseCourse.setUserId(userId);
        chooseCourse.setCompanyId(coursepublish.getCompanyId());
        chooseCourse.setOrderType("700001");//免费课程
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursepublish.getPrice());
        chooseCourse.setValidDays(365);
        chooseCourse.setStatus("701001");//选课成功
        chooseCourse.setValidtimeStart(LocalDateTime.now());//有效期的开始时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));//有效期的结束时间

        int insert = xcChooseCourseMapper.insert(chooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败");
        }

        return chooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){

        //选课成功了才可以向我的课程表添加
        String status = xcChooseCourse.getStatus();
        if(!"701001".equals(status)){
            XueChengPlusException.cast("选课没有成功无法添加到课程表");
        }
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, xcChooseCourse.getUserId()).eq(XcCourseTables::getCourseId, xcChooseCourse.getCourseId()));
        if(xcCourseTables!=null){
            return xcCourseTables;
        }

        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());//记录选课表的主键
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());//选课类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if(insert<=0){
            XueChengPlusException.cast("添加我的课程表失败");
        }

        return xcCourseTables;
    }



    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId,CoursePublish coursepublish){

        //课程id
        Long courseId = coursepublish.getId();
        //判断，如果存在收费的选课记录且选课状态为待支付，直接返回了
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700002")//收费课程
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if(!xcChooseCourses.isEmpty()){
            return xcChooseCourses.get(0);
        }

        //向选课记录表写数据
        XcChooseCourse chooseCourse = new XcChooseCourse();

        chooseCourse.setCourseId(courseId);
        chooseCourse.setCourseName(coursepublish.getName());
        chooseCourse.setUserId(userId);
        chooseCourse.setCompanyId(coursepublish.getCompanyId());
        chooseCourse.setOrderType("700002");//收费课程
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursepublish.getPrice());
        chooseCourse.setValidDays(365);
        chooseCourse.setStatus("701002");//待支付
        chooseCourse.setValidtimeStart(LocalDateTime.now());//有效期的开始时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));//有效期的结束时间

        int insert = xcChooseCourseMapper.insert(chooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败");
        }

        return chooseCourse;

    }
}
