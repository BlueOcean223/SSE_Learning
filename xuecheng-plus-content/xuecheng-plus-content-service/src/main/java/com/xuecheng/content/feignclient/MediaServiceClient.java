package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

// 远程调用media模块接口
@FeignClient(value = "media-api",configuration = {MultipartSupportConfig.class},fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {
    @RequestMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName",required = false) String objectName);
}
