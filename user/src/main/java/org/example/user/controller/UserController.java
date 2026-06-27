package org.example.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.example.client.service.InnerFileService;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.model.vo.UserVO;
import org.example.common.request.DeleteRequest;
import org.example.common.response.BaseResponse;
import org.example.common.response.ResultUtils;
import org.example.model.entity.User;
import org.example.user.model.dto.*;

import org.example.user.model.vo.LoginUserVO;

import org.example.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.user.service.impl.UserServiceImpl.SALT;

@RestController
//@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @DubboReference(check = false)
    private InnerFileService fileServiceClient;

//    @Resource
//    private WxOpenConfig wxOpenConfig;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    //region 登录相关
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)){
            return null;
        }

        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        return ResultUtils.success(loginUserVO);
    }


    /**
     * 用户注销
     *
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(){
        boolean result = userService.userLogout();

        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(){
        User user = userService.getLoginUser();

        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request){
        if(userAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        //默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());

        return ResultUtils.success(b);
    }

    /**
     * 更新用户（支持可选头像文件上传，通过 file 服务上传至腾讯云 COS）
     *
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){

        User user = new User();
        user.setId(StpUtil.getLoginIdAsLong());
        user.setUserName(userUpdateRequest.getUserName());
        user.setUserProfile(userUpdateRequest.getUserProfile());
        user.setUserAvatar(userUpdateRequest.getUserAvatar());
        user.setUserResume(userUpdateRequest.getUserResume());


        // 如果传入了新的 userAvatar URL，且与旧头像不同，则删除旧头像
        String newAvatar = userUpdateRequest.getUserAvatar();

        boolean result = userService.updateById(user);
        User oldUser = userService.getById(user.getId());
        try{
            if (StringUtils.isNotBlank(newAvatar)) {

                String oldAvatar = oldUser.getUserAvatar();
                if (StringUtils.isNotBlank(oldAvatar) && !oldAvatar.equals(newAvatar)) {
                    fileServiceClient.deleteByUrl(oldAvatar);
                }
            }
            if(StringUtils.isNotBlank(userUpdateRequest.getUserResume())){
                String oldResume = oldUser.getUserResume();
                if (StringUtils.isNotBlank(oldResume) && !oldResume.equals(userUpdateRequest.getUserResume())) {
                    fileServiceClient.deleteByUrl(oldResume);
                }

            }
        }catch (Exception e){
            log.error("updateUser error", e);
        }



        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<User> getUserById(long id, HttpServletRequest request){
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request){
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }
    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest){
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));

        return ResultUtils.success(userPage);
    }
    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request){
        if(userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current,size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current,size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);

        return ResultUtils.success(userVOPage);
    }
//    /**
//     * 更新个人信息
//     *
//     * @param userUpdateMyRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/update/my")
//    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
//                                              HttpServletRequest request){
//        if(userUpdateMyRequest == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        User user = new User();
//        BeanUtils.copyProperties(userUpdateMyRequest, user);
//        user.setId(loginUser.getId());
//        boolean result = userService.updateById(user);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//
//        return ResultUtils.success(true);
//    }

    @PostMapping("/add/sign_in")
    public BaseResponse<Boolean> addUserSignIn(HttpServletRequest request){
        User loginUser = userService.getLoginUser();
        boolean result = userService.addUserSignIn(loginUser.getId());

        return ResultUtils.success(result);
    }

    @GetMapping("/get/sign_in")
    public BaseResponse<List<Integer>> getUserSignInRecord(Integer year, HttpServletRequest request){
        User loginUser = userService.getLoginUser();
        List<Integer> userSignInRecord = userService.getUserSignInRecord(loginUser.getId(), year);

        return ResultUtils.success(userSignInRecord);
    }
}
