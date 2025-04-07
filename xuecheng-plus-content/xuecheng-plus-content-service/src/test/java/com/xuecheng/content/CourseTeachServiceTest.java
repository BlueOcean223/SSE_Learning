package com.xuecheng.content;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class CourseTeachServiceTest {

    @Resource
    private CourseTeacherInfoService courseTeacherInfoService;

    @Test
    public void testCourseTeacherList(){
        List<CourseTeacher> teachers = courseTeacherInfoService.queryTeacherList(74L);
        System.out.println(teachers);
    }

    @Test
    public void testAddTeacher(){
        CourseTeacher courseTeacher = new CourseTeacher();
        courseTeacher.setCourseId(74L);
        courseTeacher.setTeacherName("李李李老师");
        courseTeacher.setPosition("老师");
        courseTeacher.setIntroduction("老师");
        CourseTeacher result = courseTeacherInfoService.addTeacher(courseTeacher);
        System.out.println(result);
    }

    @Test
    public void testUpdateTeacher(){
        CourseTeacher courseTeacher = new CourseTeacher();
        courseTeacher.setId(21L);
        courseTeacher.setCourseId(74L);
        courseTeacher.setTeacherName("哈哈哈哈老师");
        courseTeacher.setPosition("嘻嘻老师");
        courseTeacher.setIntroduction("嘻嘻老师");
        CourseTeacher result = courseTeacherInfoService.addTeacher(courseTeacher);
        System.out.println(result);
    }
}
