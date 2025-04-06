package com.xuecheng.content.api;


import com.xuecheng.content.model.dto.SavaTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

// 课程计划管理相关接口
@Api(value = "课程计划管理相关接口", tags = "课程计划管理相关接口")
@RestController
public class TeachplanController {

    @Resource
    private TeachplanService teachplanService;

    @ApiOperation("查询课程计划树形结构")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        return teachplanService.findTeachplanTree(courseId);
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SavaTeachplanDto teachplanDto){
        teachplanService.saveTeachplan(teachplanDto);
    }
}
