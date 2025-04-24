package com.xuecheng.content.feignclient;


import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


// 降级处理
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    // 拿到了熔断的异常信息throwable
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedata, String objectName) {
                log.debug("远程调用上传文件的接口发生熔断：{}",throwable.toString(),throwable);
                return null;
            }
        };
    }
}
