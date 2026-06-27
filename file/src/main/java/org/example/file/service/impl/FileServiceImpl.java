package org.example.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.file.config.CosProperties;

import org.example.file.service.FileService;
import org.example.model.dto.file.FileUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosProperties cosProperties;

    /** 允许的头像图片类型 */
    private static final String[] AVATAR_ALLOWED_EXT = {"jpg", "jpeg", "png", "gif", "webp"};

    @Override
    public FileUploadResult uploadAvatar(MultipartFile file) {
        validateFileNotEmpty(file);
        String ext = getFileExtension(file);
        if (!StrUtil.equalsAnyIgnoreCase(ext, AVATAR_ALLOWED_EXT)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像仅支持 jpg、jpeg、png、gif、webp 格式");
        }
        return uploadFile(file, "avatar");
    }

    @Override
    public FileUploadResult uploadPdf(MultipartFile file) {
        validateFileNotEmpty(file);
        String ext = getFileExtension(file);
        if (!"pdf".equalsIgnoreCase(ext)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 PDF 格式文件");
        }
        return uploadFile(file, "pdf");
    }

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String category) {
        validateFileNotEmpty(file);

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(file);

        // 生成对象 Key：category/uuid.ext
        String objectKey = category + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putRequest = new PutObjectRequest(
                    cosProperties.getBucketName(), objectKey,
                    file.getInputStream(), metadata);

            cosClient.putObject(putRequest);

            // 构建访问 URL
            String url = buildAccessUrl(objectKey);

            log.info("文件上传成功: {} -> {}", originalFilename, url);

            return FileUploadResult.builder()
                    .url(url)
                    .objectKey(objectKey)
                    .originalFilename(originalFilename)
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "objectKey 不能为空");
        }
        try {
            cosClient.deleteObject(cosProperties.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件 URL 不能为空");
        }
        // 从 URL 中提取 objectKey
        String objectKey = extractObjectKey(fileUrl);
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法从 URL 解析出对象 Key: " + fileUrl);
        }
        deleteFile(objectKey);
    }

    /**
     * 从文件 URL 中提取 COS 对象 Key
     * <p>
     * 支持的 URL 格式：
     * <ul>
     *   <li>自定义域名：https://cdn.example.com/avatar/xxx.jpg -> avatar/xxx.jpg</li>
     *   <li>CDN 域名：https://bucket.cos.region.myqcloud.com/avatar/xxx.jpg -> avatar/xxx.jpg</li>
     * </ul>
     */
    private String extractObjectKey(String fileUrl) {
        try {
            // 去掉协议头
            String url = fileUrl;
            if (url.startsWith("https://")) {
                url = url.substring(8);
            } else if (url.startsWith("http://")) {
                url = url.substring(7);
            }

            // 找到第一个 "/" 之后的内容即为 objectKey
            int slashIndex = url.indexOf('/');
            if (slashIndex >= 0 && slashIndex < url.length() - 1) {
                return url.substring(slashIndex + 1);
            }
        } catch (Exception e) {
            log.error("解析文件 URL 失败: {}", fileUrl, e);
        }
        return null;
    }

    /**
     * 构建文件访问 URL
     */
    private String buildAccessUrl(String objectKey) {
        if (StrUtil.isNotBlank(cosProperties.getBaseUrl())) {
            // 使用自定义域名
            String baseUrl = cosProperties.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/" + objectKey;
        }
        // 使用默认 CDN 域名
        return "https://" + cosProperties.getBucketName() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + objectKey;
    }

    /**
     * 校验文件不为空
     */
    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "";
        }
        return FileUtil.extName(originalFilename);
    }

}
