package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @description 媒资文件管理业务类
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Resource
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    MediaFileService currentProxy;

    @Resource
    private TransactionTemplate transactionTemplate;

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

    }

    //根据扩展名获取mimeType
    private String getMimeType(String extension){
        if(extension == null){
            extension = "";
        }
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;

    }

    /**
     * 将文件上传到minio
     * @param inputStream 文件输入流
     * @param mimeType 媒体类型
     * @param bucket 桶
     * @param objectName 对象名
     * @return
     */
    @Override
    public boolean addMediaFilesToMinIO(InputStream inputStream,String mimeType,String bucket, String objectName){
        try {
            // 使用流的形式上传文件
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType(mimeType)
                    .stream(inputStream,inputStream.available(),-1)
                    .build();
            //上传文件
            minioClient.putObject(putObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
            return true;
        } catch (Exception e) {
           e.printStackTrace();
           log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
        }
        return false;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/")+"/";
    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, MultipartFile filedata,String objectName){

        //准备上传文件的信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //原始文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        //文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //文件类型
        uploadFileParamsDto.setFileType("001001");

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //先得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //得到mimeType
        String mimeType = getMimeType(extension);

        //子目录
        String defaultFolderPath = getDefaultFolderPath();
        //文件的md5值
        String fileMd5 = "";
        boolean result = false;
        try{
            fileMd5 = DigestUtils.md5Hex(filedata.getInputStream());
            if(StringUtils.isEmpty(objectName)) {
                // 如果文件路径为空，则使用默认的日期存储路径
                objectName = defaultFolderPath + fileMd5 + extension;
            }
            //上传文件到minio
            result = addMediaFilesToMinIO(filedata.getInputStream(), mimeType, bucket_mediafiles, objectName);
        }catch (IOException e){
            XueChengPlusException.cast("获取输入流异常");
        }
        if(!result){
            XueChengPlusException.cast("上传文件失败");
        }
        //入库文件信息
        String finalObjectName = objectName;
        String finalFileMd = fileMd5;
        MediaFiles mediaFiles = transactionTemplate.execute(status -> addMediaFilesToDb(companyId, finalFileMd,uploadFileParamsDto,bucket_mediafiles, finalObjectName));
        if(mediaFiles==null){
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

        return uploadFileResultDto;
    }


    /**
     * @description 将文件信息添加到文件表
     * @param companyId  机构id
     * @param fileMd5  文件md5值
     * @param uploadFileParamsDto  上传文件的信息
     * @param bucket  桶
     * @param objectName 对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     */
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles == null){
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/"+bucket+"/"+objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //插入数据库
            int insert = mediaFilesMapper.insert(mediaFiles);
            if(insert<=0){
                log.debug("向数据库保存文件失败,bucket:{},objectName:{}",bucket,objectName);
                return null;
            }
            // 记录待处理任务
            addWaitingTask(mediaFiles);

            return mediaFiles;
        }
        return mediaFiles;
    }

    // 添加待处理任务
    private void addWaitingTask(MediaFiles mediaFiles){
        // 文件名称
        String fileName = mediaFiles.getFilename();
        // 获取文件的mimeType
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        // 如果是avi视频则写入待处理任务
        if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            // 设置任务初始状态
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setUrl(null);
            try {
                int result = mediaProcessMapper.insert(mediaProcess);
                if (result < 0) {
                    log.error("向数据库保存待处理任务失败,{}", mediaProcess);
                }
            }catch (Exception e){
                log.error("插入数据库异常:,{}", e.getMessage());
            }
        }
    }

    // 上传前检查文件是否存在
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5){
        // 先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles != null){
            // 数据库中存在，再查询minio
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            try{
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if(inputStream != null){
                    // 文件已存在
                    return RestResponse.success(true);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        // 文件不存在
        return RestResponse.success(false);
    }

    // 检查分块文件是否存在
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunk){
        // 根据md5得到分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        // 查询minio中是否存在该分块文件
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath + chunk)
                .build();
        try{
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if(inputStream != null){
                // 文件已存在
                return RestResponse.success(true);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        // 文件不存在
        return RestResponse.success(false);
    }

    // 上传分块文件到minio
    @Override
    public RestResponse uploadChunk(MultipartFile file, String fileMd5, int chunk){
        // 获取分块文件的路径,作为文件名
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        // 获取mimeType
        String mimeType = getMimeType(null);
        boolean b = false;
        // 上传到minio
        try{
            b = addMediaFilesToMinIO(file.getInputStream(),mimeType,bucket_video,chunkFilePath);
        }catch (Exception e){
            XueChengPlusException.cast("获取输入流出错");
        }
        if(!b){
            return RestResponse.validfail("上传分块文件失败", false);
        }
        // 上传成功
        return RestResponse.success(true);
    }

    // 合并文件
    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, String fileName, int chunkTotal){
        // 文件信息对象,用于插入数据库
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(fileName);
        uploadFileParamsDto.setTags("视频文件");
        uploadFileParamsDto.setFileType("001002");

        // 找到所有分块文件
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
//        List<ComposeSource> sources = Stream.iterate(0,i->++i).limit(chunkTotal).map(i -> ComposeSource.builder().bucket(bucket_video).object(chunkFileFolderPath + i).build()).collect(Collectors.toList());
        List<ComposeSource> sources = IntStream.range(0, chunkTotal).mapToObj(i -> ComposeSource.builder().bucket(bucket_video).object(chunkFileFolderPath + i).build()).collect(Collectors.toList());
        // 扩展名
        String extension = fileName.substring(fileName.lastIndexOf("."));
        // 合并后文件的objectName等信息
        String objectName = getFilePathByMd5(fileMd5, extension);

        // 利用minio合并文件
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)
                .sources(sources)
                .build();
        try{
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e){
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}",bucket_video,objectName,e.getMessage());
            return RestResponse.validfail("合并文件出错", false);
        }

        // 获取合并后的文件大小
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket_video)
                    .object(objectName)
                    .build());
            long fileSize = stat.size();
            uploadFileParamsDto.setFileSize(fileSize);  // 设置文件大小到DTO
        } catch (Exception e) {
            log.error("获取文件大小失败:{}", e.getMessage());
            return RestResponse.validfail("获取文件大小失败", false);
        }

        // 校验合并后的文件和源文件是否一致
        try{
            // 计算合并后的文件的md5值
            InputStream fileInputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket_video).object(objectName).build());
            String mergeFileMd5 = DigestUtils.md5Hex(fileInputStream);
            if(!fileMd5.equals(mergeFileMd5)){
                log.error("校验合并文件md5值不一致，原始文件:{},合并文件:{}",fileMd5,mergeFileMd5);
                return RestResponse.validfail("合并文件校验出错", false);
            }
        } catch (Exception e){
            return RestResponse.validfail("合并文件校验出错", false);
        }

        // 将文件消息加入数据库
        MediaFiles mediaFiles = transactionTemplate.execute(status -> addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName));
        if(mediaFiles == null){
            return RestResponse.validfail("文件信息入库失败", false);
        }

        // 清理分块文件
        clearChunkFiles(chunkFileFolderPath,chunkTotal);
        return RestResponse.success(true);
    }

    // 清理分块文件
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal){
        try {
            // 1. 使用IntStream替代Stream.iterate更简洁
            List<DeleteObject> objects = IntStream.range(0, chunkTotal)
                    .mapToObj(i -> new DeleteObject(chunkFileFolderPath + i))
                    .collect(Collectors.toList());

            // 2. 构建删除参数
            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                    .bucket(bucket_video)
                    .objects(objects)
                    .build();

            // 3. 执行删除并处理结果
            minioClient.removeObjects(removeObjectsArgs)
                    .forEach(result -> {
                        try {
                            // 显式获取结果以确保删除操作完成
                            result.get();
                        } catch (Exception e) {
                            log.error("删除分块文件失败: {}", e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("清楚分块文件时发生异常: {}", e.getMessage());
            XueChengPlusException.cast("清楚分块文件时发生异常");
        }
    }


    // 从minio下载文件
    @Override
    public File downloadFileFromMinio(String bucket, String objectName){
        // 临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch(Exception e){
            log.error("文件下载出错:{}", e.getMessage());
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("文件输出流关闭失败:{}", e.getMessage());
                }
            }
        }
        return null;
    }

    // 根据媒资id查询媒资信息
    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }

    // 合并后的文件路径
    private String getFilePathByMd5(String fileMd5, String extension){
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" +fileMd5 +extension;
    }

    // 得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

}
