package org.example.agent.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.model.dto.file.FileUploadResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 通过 HTTP 调用 file 模块的文件上传服务
 */
@Slf4j
@Service
public class FileServiceClient {

    @Resource
    private RestTemplate restTemplate;

    /**
     * 上传 PDF 到 file 服务（腾讯云 COS）
     *
     * @param file PDF 文件
     * @return 上传结果（包含 COS URL）
     */
    public FileUploadResult uploadPdf(MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 使用 Nacos 服务名调用 file 服务
            ResponseEntity<FileUploadResult> response = restTemplate.exchange(
                    "http://file/file/upload/pdf", HttpMethod.POST, requestEntity, FileUploadResult.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传服务调用失败");
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        }
    }

}
