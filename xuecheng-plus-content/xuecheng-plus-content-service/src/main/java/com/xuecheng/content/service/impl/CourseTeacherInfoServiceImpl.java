package com.xuecheng.content.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

// 课程教师管理接口
@Slf4j
@Service
public class CourseTeacherInfoServiceImpl implements CourseTeacherInfoService {

    @Resource
    private CourseTeacherMapper courseTeacherMapper;

    /**
     * 查询教师列表
     * @param courseId 课程id
     * @return 教师列表
     */
    @Override
    public List<CourseTeacher> queryTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    /**
     * 添加或修改教师
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    @Override
    public CourseTeacher addTeacher(CourseTeacher courseTeacher) {
        if(courseTeacher == null){
            XueChengPlusException.cast("教师信息为空，无法操作");
        }
        Long id = courseTeacher.getId();
        if(id == null){
            // 新增教师
            int result = courseTeacherMapper.insert(courseTeacher);
            if(result <= 0){
                XueChengPlusException.cast("新增教师失败");
            }
        }else{
            // 修改教师
            int result = courseTeacherMapper.updateById(courseTeacher);
            if(result <= 0){
                XueChengPlusException.cast("修改教师失败");
            }
        }
        return courseTeacher;
    }

    /**
     * 删除教师
     * @param courseId 课程id
     * @param id 教师id
     */
    @Override
    public void deleteTeacher(Long courseId, Long id) {
        if(id == null){
            XueChengPlusException.cast("教师id为空，无法操作");
        }
        if(courseId == null){
            XueChengPlusException.cast("课程id为空，无法操作");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        queryWrapper.eq(CourseTeacher::getId, id);
        int result = courseTeacherMapper.delete(queryWrapper);
        if(result <= 0){
            XueChengPlusException.cast("删除教师失败");
        }
    }
}
