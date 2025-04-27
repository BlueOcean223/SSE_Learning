package com.xuecheng.search.service;

import com.xuecheng.search.po.CourseIndex;

/**
 * 课程索引service
 */
public interface IndexService {

    /**
     * 添加索引
     * @param indexName 索引名称
     * @param id 主键
     * @param object 索引对象
     * @return Boolean true表示成功,false失败
     */
    public Boolean addCourseIndex(String indexName,String id,Object object);


    /**
     * 更新索引
     * @param indexName 索引名称
     * @param id 主键
     * @param object 索引对象
     * @return Boolean true表示成功,false失败
    */
    public Boolean updateCourseIndex(String indexName,String id,Object object);

    /**
     * 删除索引
     * @param indexName 索引名称
     * @param id  主键
     * @return java.lang.Boolean
    */
    public Boolean deleteCourseIndex(String indexName,String id);

}
