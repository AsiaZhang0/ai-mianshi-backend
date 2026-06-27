package org.example.file.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.example.client.service.InnerFileService;
import org.example.file.service.FileService;

/**
 * 文件服务 Dubbo 暴露实现
 */
@Slf4j
@DubboService
public class InnerFileServiceImpl implements InnerFileService {

    @Resource
    private FileService fileService;

    @Override
    public boolean deleteByUrl(String fileUrl) {
        fileService.deleteByUrl(fileUrl);
        return true;
    }

    @Override
    public boolean deleteByObjectKey(String objectKey) {
        fileService.deleteFile(objectKey);
        return true;
    }
}
