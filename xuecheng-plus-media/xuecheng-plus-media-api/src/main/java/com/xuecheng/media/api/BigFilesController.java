package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

// 大文件上传接口，上传视频
@Api(value = "大文件上传接口",tags = "大文件上传接口")
@RestController
public class BigFilesController {

    @Resource
    private MediaFileService mediaFileService;

    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkFile(@RequestParam String fileMd5) throws Exception {
        return mediaFileService.checkFile(fileMd5);
    }

    @ApiOperation(value = "分块文件上传前的检查")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkChunk(@RequestParam String fileMd5, @RequestParam int chunk) throws Exception {
        return mediaFileService.checkChunk(fileMd5, chunk);
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadChunk(@RequestParam("file") MultipartFile file, @RequestParam String fileMd5, @RequestParam int chunk) throws Exception {
        return mediaFileService.uploadChunk(file, fileMd5, chunk);
    }

    @ApiOperation(value = "合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergeChunks(@RequestParam String fileMd5, @RequestParam String fileName, @RequestParam int chunkTotal) throws Exception {
        Long companyId = 1232141425L;
        return mediaFileService.mergeChunks(companyId,fileMd5, fileName, chunkTotal);
    }
}
