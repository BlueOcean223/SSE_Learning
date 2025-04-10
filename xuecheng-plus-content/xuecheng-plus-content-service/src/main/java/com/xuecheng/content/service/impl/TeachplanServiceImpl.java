package com.xuecheng.content.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SavaTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Resource
    private TeachplanMapper teachplanMapper;

    @Resource
    private TeachplanMediaMapper teachplanMediaMapper;

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
            // 按排序字段排序，确保列表最后一个元素的排序字段最大
            teachplanList.sort(Comparator.comparingInt(Teachplan::getOrderby));
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

    /**
     * 删除课程计划章节
     * @param teachplanId 课程计划id
     *
     */
    @Transactional
    @Override
    public void deleteTeachplan(Long teachplanId){
        if(teachplanId == null){
            XueChengPlusException.cast("课程计划id为空");
        }
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan == null){
            XueChengPlusException.cast("课程计划不存在");
        }
        if(teachplan.getParentid() == 0){
            // 删除第一级别章节
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,teachplanId);
            int count = teachplanMapper.selectCount(queryWrapper);
            if(count > 0){
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
            }
            int result = teachplanMapper.deleteById(teachplanId);
            if(result <= 0){
                XueChengPlusException.cast("删除课程计划失败");
            }
        }else {
            // 删除第二级别章节
            int result = teachplanMapper.deleteById(teachplanId);
            if(result <= 0){
                XueChengPlusException.cast("删除课程计划失败");
            }
            // 查询课程计划章节视频信息
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);
            if(teachplanMedia != null){
                // 课程计划章节视频信息存在，需要删除
                int result1 = teachplanMediaMapper.delete(queryWrapper);
                if(result1 <= 0 ){
                    XueChengPlusException.cast("删除课程计划视频信息失败");
                }
            }
        }
    }

    /**
     * 课程计划上移下移
     * @param moveType 上移下移类型
     * @param teachplanId 课程计划id
     */
    @Transactional
    @Override
    public void moveTeachplan(String moveType, Long teachplanId) {
        if(teachplanId == null){
            XueChengPlusException.cast("课程计划id为空");
        }
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan == null){
            XueChengPlusException.cast("课程计划不存在");
        }

        // 获取课程计划在同属章节的顺序
        int index = 0;
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid,teachplan.getParentid());
        List<Teachplan> teachplanList = teachplanMapper.selectList(queryWrapper);
        if(teachplanList.size() == 1){
            // 只有该节点，不做任何操作
            return;
        }
        // 将列表按排序字段排序
        teachplanList.sort(Comparator.comparingInt(Teachplan::getOrderby));
        for(int i=0;i<teachplanList.size();i++){
            if(teachplanList.get(i).getId().equals(teachplanId)){
                index = i;
                break;
            }
        }

        if(moveType.equals("moveup")){
            // 上移
            if(index == 0){
                // 已经处于第一个，不做任何操作
                return;
            }
            // 交换排序字段
            Teachplan teachplanPrev = teachplanList.get(index-1);
            Integer orderbyPrev = teachplanPrev.getOrderby();
            teachplanPrev.setOrderby(teachplan.getOrderby());
            teachplan.setOrderby(orderbyPrev);
            // 更新数据库
            int result = teachplanMapper.updateById(teachplanPrev);
            result += teachplanMapper.updateById(teachplan);
            if(result < 2){
                XueChengPlusException.cast("课程计划上移失败");
            }
        }else{
            // 下移
            if(index == teachplanList.size()-1){
                // 已经处于最后一个，不做任何操作
                return;
            }
            Teachplan teachplanNext = teachplanList.get(index+1);
            Integer orderbyNext = teachplanNext.getOrderby();
            teachplanNext.setOrderby(teachplan.getOrderby());
            teachplan.setOrderby(orderbyNext);
            // 更新数据库
            int result = teachplanMapper.updateById(teachplanNext);
            result += teachplanMapper.updateById(teachplan);
            if(result < 2){
                XueChengPlusException.cast("课程计划下移失败");
            }
        }
    }
}
