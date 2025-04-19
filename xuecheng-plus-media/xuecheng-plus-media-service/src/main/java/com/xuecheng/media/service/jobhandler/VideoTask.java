package com.xuecheng.media.service.jobhandler;


import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 任务处理类
 */
@lombok.extern.slf4j.Slf4j
@Slf4j
@Component
public class VideoTask {

    @Resource
    private MediaFileProcessService mediaFileProcessService;

    @Resource
    private MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    // 视频处理任务
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 确定cpu核心数
        int processors = Runtime.getRuntime().availableProcessors();
        // 查询待处理任务
        List<MediaProcess> mediaProcessList = null;
        try {
            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        }catch (Exception e){
            System.out.println("查询数据库异常"+e.getMessage());
        }
        // 任务数量
        int size = mediaProcessList.size();
        log.debug("查询到待处理任务数：{}", size);
        if(size <= 0){
            return;
        }
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(processors);
        // 使用计数器，保证所有线程完成后函数才结束
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            // 将任务加入线程池
            executorService.execute(() -> {
                File file = null;
                File mp4File = null;
                try {
                    Long taskId = mediaProcess.getId();
                    String fileId = mediaProcess.getFileId();
                    // 开启任务
                    boolean startTask = mediaFileProcessService.startTask(taskId);
                    if (!startTask) {
                        log.debug("抢占任务失败，任务id：{}", taskId);
                        return;
                    }

                    // 桶
                    String bucket = mediaProcess.getBucket();
                    // objectName
                    String objectName = mediaProcess.getFilePath();
                    // 下载minio视频
                    file = mediaFileService.downloadFileFromMinio(bucket, objectName);
                    if (file == null) {
                        log.debug("下载视频失败，任务id：{}，bucket:{},objectName:{}", taskId, bucket, objectName);
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频失败");
                        return;
                    }

                    // 原avi视频路径
                    String videoPath = file.getAbsolutePath();
                    // 转换后mp4文件的名称
                    String mp4Name = fileId + ".mp4";
                    // 转换后mp4文件的路径
                    try{
                        mp4File = File.createTempFile("minio",".mp4");
                    } catch (IOException e){
                        log.debug("创建临时文件失败：{}",e.getMessage() );
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件失败");
                        return;
                    }
                    String mp4Path = mp4File.getAbsolutePath();
                    // 使用工具类对象转换视频
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, videoPath, mp4Name, mp4Path);
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")){
                        log.debug(("视频转码失败，原因：{}，bucket:{},objectName:{}"),result,bucket,objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        return;
                    }

                    // 上传到minio
                    String mp4ObejctName = objectName.substring(0, objectName.lastIndexOf(".")) + ".mp4";
                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(mp4File);
                    }catch (Exception e){
                        log.debug("读取mp4临时文件失败：{}",e.getMessage());
                        return;
                    }
                    boolean result1 = mediaFileService.addMediaFilesToMinIO(inputStream, "video/mp4", bucket, mp4ObejctName);
                    if(!result1){
                        log.debug("上传mp4文件失败，taskId:{}",taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "上传mp4文件失败");
                        return;
                    }
                    // 在minio中的url
                    String url = getFilePath(fileId, ".mp4");
                    // 更新任务状态为成功
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } catch (Exception e) {
                    log.debug("执行视频处理任务失败，错误信息:{}",e.getMessage());
                } finally {
                    // 删除临时文件
                    try {
                        file.delete();
                        mp4File.delete();
                    }catch (Exception e){
                        log.debug("删除临时文件失败：{}",e.getMessage());
                    }
                    // 计数器减一
                    countDownLatch.countDown();
                }
            });
        });

        // 阻塞，指定最大限制的等待时间，阻塞最多等待一定的时间后就解除阻塞
        countDownLatch.await(1, TimeUnit.MINUTES);
    }

    // 获取文件在minio的存储路径
    private String getFilePath(String fileMd5,String fileExt){
        return   fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }
}
