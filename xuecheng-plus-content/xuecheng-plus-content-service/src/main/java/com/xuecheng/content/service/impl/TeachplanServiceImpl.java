package com.xuecheng.content.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SavaTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.Collections;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Resource
    private TeachplanMapper teachplanMapper;

    /**
     * 根据课程id查询课程计划
     * @param courseId 课程计划
     * @return 课程计划信息
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto 课程计划信息
     */
    @Override
    public void saveTeachplan(SavaTeachplanDto saveTeachplanDto) {
        Long teachplanId = saveTeachplanDto.getId();
        if(teachplanId == null){
            // 新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            // 确定排序字段
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId,saveTeachplanDto.getCourseId())
                    .eq(Teachplan::getParentid,saveTeachplanDto.getParentid());
            List<Teachplan> teachplanList = teachplanMapper.selectList(queryWrapper);
            Integer orderby = !teachplanList.isEmpty() ? teachplanList.get(teachplanList.size()-1).getOrderby()+1 : 1;
            teachplan.setOrderby(orderby);

            // 保存至数据库
            int result = teachplanMapper.insert(teachplan);
            if(result <= 0){
                XueChengPlusException.cast("新增课程计划失败");
            }
        } else{
            // 修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            // 更新至数据库
            int result = teachplanMapper.updateById(teachplan);
            if(result <= 0){
                XueChengPlusException.cast("修改课程计划失败");
            }
        }
    }
}
