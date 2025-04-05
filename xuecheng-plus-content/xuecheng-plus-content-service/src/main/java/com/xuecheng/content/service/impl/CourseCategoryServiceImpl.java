package com.xuecheng.content.service.impl;


import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


// 课程分类查询
@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        // 调用mapper查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNode(id);
        //找到每个节点的子节点，最终封装成返回结果
        //先将list转成map，key是节点id，value是节点对象，方便获取对象
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(CourseCategory::getId, value->value,(key1,key2)->key2));
        //结果list
        List<CourseCategoryTreeDto> result = new ArrayList<>();
        //一边遍历一边将子节点放入父结点的childrenTreeNodes中
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            // 结果集合中只包含第一层节点
            if(item.getParentid().equals(id)){
                result.add(item);
            }
            // 找到节点的父结点
            CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
            if(courseCategoryParent!=null){
                // 如果父结点的子节点集合为空，创建集合
                if(courseCategoryParent.getChildrenTreeNodes() == null){
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<>());
                }
                // 将每个子节点放到父结点的子节点集合中
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }
        });
        return result;
    }
}
