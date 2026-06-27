package org.example.file.service;


import org.example.model.dto.file.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileService {

    /**
     * 上传头像图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    FileUploadResult uploadAvatar(MultipartFile file);

    /**
     * 上传 PDF 简历
     *
     * @param file PDF 文件
     * @return 上传结果
     */
    FileUploadResult uploadPdf(MultipartFile file);

    /**
     * 通用文件上传
     *
     * @param file     文件
     * @param category 文件分类目录（如 avatar, pdf）
     * @return 上传结果
     */
    FileUploadResult uploadFile(MultipartFile file, String category);

    /**
     * 删除文件
     *
     * @param objectKey COS 对象 Key
     */
    void deleteFile(String objectKey);

    /**
     * 根据文件 URL 删除文件
     * 自动从 URL 中解析出 COS 对象 Key 再删除
     *
     * @param fileUrl 文件访问 URL
     */
    void deleteByUrl(String fileUrl);

}
