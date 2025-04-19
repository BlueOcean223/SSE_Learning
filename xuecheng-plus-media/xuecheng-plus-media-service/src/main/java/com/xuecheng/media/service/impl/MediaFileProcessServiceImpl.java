package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Resource
    private MediaProcessMapper mediaProcessMapper;

    @Resource
    private MediaFilesMapper mediaFilesMapper;

    @Resource
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;


    // 获取待处理任务
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count){
        return mediaProcessMapper.selectListByShardIndex(shardIndex, shardTotal, count);
    }

    // 开启任务
    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result > 0;
    }

    // 保存任务执行结果
    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg){
        // 要更新的任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess == null){
            return;
        }
        // 任务执行失败
        if(status.equals("3")){
            if(errorMsg.length() > 1024){
                errorMsg = "执行失败且失败消息过长";
            }
            LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MediaProcess::getId,taskId)
                    .set(MediaProcess::getStatus,status)
                    .set(MediaProcess::getFailCount,mediaProcess.getFailCount()+1)
                    .set(MediaProcess::getErrormsg,errorMsg);
            mediaProcessMapper.update(null,updateWrapper);
            return;
        }
        // 任务执行成功
        // 文件表记录
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);
        // 更新MediaProcess表的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);
        // 插入MediaProcessHistory表
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        // 从MediaProcess表删除当前任务
        mediaProcessMapper.deleteById(taskId);
    }
}
