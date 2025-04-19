package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

// 任务处理接口
public interface MediaFileProcessService {

    // 获取待处理任务
    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 开启一个任务
     * @param id 任务id
     * @return  true开启任务成功，false开启任务失败
     */
    boolean startTask(long id);

    // 保存任务执行结果
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);
}
