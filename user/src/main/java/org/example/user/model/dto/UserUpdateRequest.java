package org.example.user.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像（URL路径，不上传文件时使用）
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色： user/admin/ban
     */
    private String userRole;

    /**
     * 用户简历
     */
    private String userResume;

    /**
     * 头像文件（可选上传）
     */
    private MultipartFile avatarFile;

    private static final long serialVersionUID = 1L;

}
