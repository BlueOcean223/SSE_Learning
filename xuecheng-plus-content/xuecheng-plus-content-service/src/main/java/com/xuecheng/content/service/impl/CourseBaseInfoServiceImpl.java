package com.xuecheng.content.service.impl;

// 课程信息管理接口

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    private CourseBaseMapper courseBaseMapper;

    @Resource
    private CourseMarketMapper courseMarketMapper;

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Resource
    private TeachplanMapper teachplanMapper;

    @Resource
    private TeachplanMediaMapper teachplanMediaMapper;

    @Resource
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto courseParamsDto) {
        // 查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询课程名称
        queryWrapper.like(StringUtils.isNotBlank(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
        // 审核状态
        queryWrapper.eq(StringUtils.isNotBlank(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());
        // 按课程发布状态查询
        queryWrapper.eq(StringUtils.isNotBlank(courseParamsDto.getPublishStatus()),CourseBase::getStatus,courseParamsDto.getPublishStatus());
        // 按机构id查询
        queryWrapper.eq(companyId != null,CourseBase::getCompanyId,companyId);


        // 分页查询参数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        List<CourseBase> items = pageResult.getRecords();
        long total = pageResult.getTotal();

        return new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    /**
     * 新增课程
     * @param companyId 机构id
     * @param addCourseDto 课程信息
     * @return 课程详情信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        // 向课程基本信息表写入数据
        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(addCourseDto, courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        // 审核状态默认为未提交
        courseBaseNew.setAuditStatus("202002");
        // 发布状态默认为未发布
        courseBaseNew.setStatus("203001");
        // 插入数据库
        int insert = courseBaseMapper.insert(courseBaseNew);
        if(insert<=0){
            XueChengPlusException.cast("添加课程失败");
        }

        // 向课程营销表写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        // 将页面输入的数据拷贝到courseMarket
        BeanUtils.copyProperties(addCourseDto, courseMarketNew);
        // 课程id
        courseMarketNew.setId(courseBaseNew.getId());
        // 保存营销信息
        int saveResult = saveCourseMarket(courseMarketNew);
        if(saveResult<=0){
            XueChengPlusException.cast("添加课程营销信息失败");
        }
        // 从数据库查询课程的详细信息
        return getCourseBaseInfo(courseBaseNew.getId());
    }

    /** 根据id查询课程信息
     * @param courseId 课程id
     * @return 课程信息
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        // 从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        // 从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 组装
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        // 查询分类信息，将分类名称放入courseBaseInfoDto中
        CourseCategory Mt = courseCategoryMapper.selectById(courseBase.getMt());
        CourseCategory St = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setMtName(Mt.getName());
        courseBaseInfoDto.setStName(St.getName());

        return courseBaseInfoDto;
    }

    // 营销信息，存在则更新，不存在则添加
    private int saveCourseMarket(CourseMarket courseMarketNew){
        // 参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if(StringUtils.isBlank(charge)){
            XueChengPlusException.cast("收费规则为空");
        }
        // 如果课程收费，价格没有填写也需要抛出异常
        if(charge.equals("201001")){
            if(courseMarketNew.getPrice() == null || courseMarketNew.getPrice() <= 0){
                XueChengPlusException.cast("课程价格不能为空并且必须大于0");
            }
        }
        // 从数据库查询营销信息，存在则更新，不存在则添加
        Long id = courseMarketNew.getId();
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if(courseMarket == null){
            //插入数据库
            return courseMarketMapper.insert(courseMarketNew);
        }else{
            //更新
            return courseMarketMapper.updateById(courseMarketNew);
        }
    }


    /**
     * 修改课程信息
     * @param companyId 机构id
     * @param editCourseDto 修改课程信息
     * @return 课程详情信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        // 拿到课程id
        Long courseId = editCourseDto.getId();
        // 查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            XueChengPlusException.cast("课程不存在");
        }
        // 数据合法性校验
        if(!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        // 封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        // 更新课程基本信息
        int update1 = courseBaseMapper.updateById(courseBase);
        if(update1 <= 0){
            XueChengPlusException.cast("修改课程基本信息失败");
        }
        // 更新营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket == null) {
            XueChengPlusException.cast("课程营销信息不存在");
        }
        BeanUtils.copyProperties(editCourseDto,courseMarket);
        int update2 = courseMarketMapper.updateById(courseMarket);
        if(update2 <= 0){
            XueChengPlusException.cast("修改课程营销信息失败");
        }
        // 返回课程信息
        return getCourseBaseInfo(courseId);
    }

    /**
     * 删除课程信息
     * @param courseId 课程id
     */
    @Transactional
    @Override
    public void deleteCourseBase(Long courseId) {
        if(courseId == null){
            XueChengPlusException.cast("课程id为空，无法操作");
        }
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            XueChengPlusException.cast("课程不存在,无法删除");
        }
        // 只有未提交的课程可以删除
        if(!courseBase.getAuditStatus().equals("202002")){
            XueChengPlusException.cast("只有未提交的课程可以删除");
        }
        // 删除课程基本信息
        int result1 = courseBaseMapper.deleteById(courseId);
        if(result1 <= 0){
            XueChengPlusException.cast("删除课程基本信息失败");
        }
        // 删除课程营销信息
        int result2 = courseMarketMapper.deleteById(courseId);
        if(result2 <= 0){
            XueChengPlusException.cast("删除课程营销信息失败");
        }
        // 删除课程计划
        deleteTeachplanAndMedia(courseId);
        // 删除课程教师信息
        deleteTeacher(courseId);
    }

    // 删除课程计划和相关联的媒资
    private void deleteTeachplanAndMedia(Long courseId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        List<Teachplan> teachplanList = teachplanMapper.selectList(queryWrapper);
        if(teachplanList.isEmpty()){
            // 课程计划不存在，不进行操作
            return;
        }
        int result = teachplanMapper.delete(queryWrapper);
        if(result <= 0){
            XueChengPlusException.cast("删除课程计划失败");
        }
        // 删除相关的媒资信息
        List<Long> teachplanIdList = teachplanList.stream().map(Teachplan::getId).collect(Collectors.toList());
        LambdaQueryWrapper<TeachplanMedia> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(TeachplanMedia::getTeachplanId,teachplanIdList);
        // 查询媒资信息是否存在
        List<TeachplanMedia> teachplanMediaList = teachplanMediaMapper.selectList(queryWrapper1);
        if(teachplanMediaList.isEmpty()){
            // 媒资信息不存在，不进行操作
            return;
        }
        int result1 = teachplanMediaMapper.delete(queryWrapper1);
        if(result1 <= 0){
            XueChengPlusException.cast("删除课程媒资失败");
        }
    }
    // 删除教师信息
    private void deleteTeacher(Long courseId){
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeacherList = courseTeacherMapper.selectList(queryWrapper);
        if(courseTeacherList.isEmpty()){
            // 教师信息不存在，不进行操作
            return;
        }
        int result = courseTeacherMapper.delete(queryWrapper);
        if(result <= 0){
            XueChengPlusException.cast("删除教师信息失败");
        }
    }
}
