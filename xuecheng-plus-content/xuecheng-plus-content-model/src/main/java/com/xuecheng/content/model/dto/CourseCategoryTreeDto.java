package com.xuecheng.content.model.dto;


import com.xuecheng.content.model.po.CourseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCategoryTreeDto extends CourseCategory implements java.io.Serializable {

    List<CourseCategoryTreeDto> childrenTreeNodes;
}
