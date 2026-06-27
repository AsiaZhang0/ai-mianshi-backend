package org.example.user.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.common.request.PageRequest;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;



    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色: user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;

}
