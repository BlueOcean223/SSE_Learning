package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

// 课程计划信息模型类
@Data
public class TeachplanDto extends Teachplan {
    // 与媒资管理的信息
    private TeachplanMedia teachplanMedia;
    // 小章节list
    private List<TeachplanDto> teachPlanTreeNodes;
}
