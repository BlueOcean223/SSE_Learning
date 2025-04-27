package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 课程索引管理接口实现
 */
@Slf4j
@Service
public class IndexServiceImpl implements IndexService {



 @Resource
 private RestHighLevelClient client;

 @Override
 public Boolean addCourseIndex(String indexName,String id,Object object) {
  IndexRequest indexRequest = new IndexRequest(indexName).id(id);
  //指定索引文档内容
  indexRequest.source(JSON.toJSONString(object),XContentType.JSON);
  //索引响应对象
  IndexResponse indexResponse = null;
  try {
   indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("添加索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("添加索引出错");
  }
  String name = indexResponse.getResult().name();
  System.out.println(name);
  return name.equalsIgnoreCase("created") || name.equalsIgnoreCase("updated");

 }

 @Override
 public Boolean updateCourseIndex(String indexName,String id,Object object) {

  UpdateRequest updateRequest = new UpdateRequest(indexName, id);
  updateRequest.doc(JSON.toJSONString(object), XContentType.JSON);
  UpdateResponse updateResponse = null;
  try {
   updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("更新索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("更新索引出错");
  }
  DocWriteResponse.Result result = updateResponse.getResult();
  return result.name().equalsIgnoreCase("updated");

 }

 @Override
 public Boolean deleteCourseIndex(String indexName,String id) {

  //删除索引请求对象
  DeleteRequest deleteRequest = new DeleteRequest(indexName,id);
  //响应对象
  DeleteResponse deleteResponse = null;
  try {
   deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("删除索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("删除索引出错");
  }
  //获取响应结果
  DocWriteResponse.Result result = deleteResponse.getResult();
  return result.name().equalsIgnoreCase("deleted");
 }
}
