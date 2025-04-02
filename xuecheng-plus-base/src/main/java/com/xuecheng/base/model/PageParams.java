package com.xuecheng.base.model;


//  分页查询参数

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PageParams {
    // 当前页码
    @ApiModelProperty("页码")
    private Long pageNo = 1L;
    // 每页记录数
    @ApiModelProperty("每页记录数")
    private Long pageSize = 10L;
}
