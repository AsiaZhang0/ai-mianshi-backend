package org.example.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.response.BaseResponse;
import org.example.common.response.ResultUtils;

import org.example.file.service.FileService;
import org.example.model.dto.file.FileUploadResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务 Controller
 * 为 user 模块（头像上传）和 agent 模块（PDF 上传）提供文件存储能力
 */
@Tag(name = "文件上传服务", description = "提供头像、PDF 等文件的上传与删除功能")
@Slf4j
@RestController
public class FileController {

    @Resource
    private FileService fileService;

    /**
     * 上传头像图片
     * 供 user 模块调用
     */
    @Operation(summary = "上传头像图片")
    @PostMapping("/upload/avatar")
    public BaseResponse<FileUploadResult> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能为空");
        }
        FileUploadResult result = fileService.uploadAvatar(file);
        return ResultUtils.success(result);
    }

    /**
     * 上传 PDF 简历
     * 供 agent 模块调用
     */
    @Operation(summary = "上传 PDF 简历")
    @PostMapping("/upload/pdf")
    public BaseResponse<FileUploadResult> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "PDF 文件不能为空");
        }
        FileUploadResult result = fileService.uploadPdf(file);
        return ResultUtils.success(result);
    }

    /**
     * 通用文件上传
     *
     * @param file     文件
     * @param category 文件分类目录（如 avatar, pdf, document 等）
     */
    @PostMapping("/upload")
    public BaseResponse<FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "category", defaultValue = "common") String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        FileUploadResult result = fileService.uploadFile(file, category);
        return ResultUtils.success(result);
    }

    /**
     * 删除文件
     *
     * @param objectKey COS 对象 Key
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFile(@RequestParam("objectKey") String objectKey) {
        fileService.deleteFile(objectKey);
        return ResultUtils.success(true);
    }

}
