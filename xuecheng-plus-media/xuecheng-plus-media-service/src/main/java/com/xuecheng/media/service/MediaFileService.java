package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import io.minio.UploadObjectArgs;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

 /**
  * 上传文件
  * @param companyId 机构id
  * @param filedata 文件数据
  * @return UploadFileResultDto
  */
 public UploadFileResultDto uploadFile(Long companyId, MultipartFile filedata);

 // 添加文件到数据库
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);

 // 上传前检查文件是否存在
 RestResponse<Boolean> checkFile(String fileMd5);

 // 检查分块文件是否存在
 RestResponse<Boolean> checkChunk(String fileMd5, int chunk);

 // 上传分块文件
 RestResponse uploadChunk(MultipartFile file, String fileMd5, int chunk);

 // 合并文件
 RestResponse mergeChunks(Long companyId, String fileMd5, String fileName, int chunkTotal);

 // 上传minio
 boolean addMediaFilesToMinIO(InputStream inputStream,String mimeType,String bucket, String objectName);

 // 从minio下载文件
 File downloadFileFromMinio(String bucket, String objectName);

 // 根据媒资id查询媒资信息
 MediaFiles getFileById(String mediaId);
}
