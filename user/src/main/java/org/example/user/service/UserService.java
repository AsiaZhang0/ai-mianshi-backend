package org.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.example.model.vo.UserVO;
import org.example.model.entity.User;
import org.example.user.model.dto.UserQueryRequest;

import org.example.user.model.vo.LoginUserVO;


import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return
     */
    long userRegister(String userAccount, String userPassword,String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

//    /**
//     * 用户登录（微信开放平台）
//     * @param wxOAuth2UserInfo 从微信获取的用户信息
//     * @param request
//     * @return 脱敏后的用户信息
//     */
//    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    User getLoginUser();


    /**
     * 获取当前登录用户（允许未登录）
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);
    /**
     * 是否为管理员
     *
     *
     * @return
     */

    boolean isAdmin();

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @return
     */
    boolean userLogout();

    /**
     * 获取脱敏的已登陆用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);
    /**
     * 获取脱敏的用户信息
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 添加用户记录
     * @param userId
     * @return
     */
    boolean addUserSignIn(long userId);

    List<Integer> getUserSignInRecord(long userId, Integer year);

}
