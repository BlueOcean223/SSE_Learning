package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 * @version 1.0
 */
 @Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
 @RestController
public class MediaFilesController {


  @Autowired
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
      Long companyId = 1232141425L;
      return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);
 }

 @ApiOperation("上传图片")
 @RequestMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public UploadFileResultDto upload(@RequestPart("filedata")MultipartFile filedata,
                                  @RequestParam(value = "objectName",required = false) String objectName){
     Long companyId = 1232141425L;
     //调用service上传图片
     return mediaFileService.uploadFile(companyId,filedata,objectName);
 }

}
