package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

// 课程教师管理接口
@Api(value = "课程教师管理接口", tags = "课程教师管理接口")
@RestController
public class CourseTeacherController {

    @Resource
    private CourseTeacherInfoService courseTeacherInfoService;

    @ApiOperation("查询教室列表")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable Long courseId) {
        return courseTeacherInfoService.queryTeacherList(courseId);
    }

    @ApiOperation("添加或修改教师接口")
    @PostMapping("/courseTeacher")
    public CourseTeacher addTeacher(@RequestBody CourseTeacher courseTeacher){
        return courseTeacherInfoService.addTeacher(courseTeacher);
    }

    @ApiOperation("删除教师接口")
    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteTeacher(@PathVariable Long courseId, @PathVariable Long id){
        courseTeacherInfoService.deleteTeacher(courseId, id);
    }
}
