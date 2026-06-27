package org.example.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.constant.CommonConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.SqlUtils;
import org.example.model.vo.UserVO;
import org.example.model.entity.User;
import org.example.user.constant.RedisConstant;
import org.example.user.enums.UserRoleEnum;
import org.example.user.mapper.UserMapper;
import org.example.user.model.dto.UserQueryRequest;

import org.example.user.model.vo.LoginUserVO;

import org.example.user.service.UserService;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

import static org.example.user.constant.UserConstant.USER_LOGIN_STATE;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "zhiming";
    @Resource
    RedissonClient redissonClient;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //密码和校验密码相同
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }
        synchronized(userAccount.intern()){
            //账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount",userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if(count > 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");
            }
            //加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

            //插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if(!saveResult){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号错误");
        }
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        }
        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        //用户不存在
        if(user == null){
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }
        // 检查封禁
        String userRole = user.getUserRole();
        if ("ban".equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "账号已被封禁，无法登录");
        }

        // 记录用户的登录态
        // 先踢掉该用户之前的登录（如果有），然后重新登录，生成新 Token
        StpUtil.kickout(user.getId());
        StpUtil.login(user.getId());
        // 只存 userId 而非整个 User 对象，避免 Gateway 反序列化时找不到 User 类
//        StpUtil.getSession().set(USER_LOGIN_STATE, user.getId());
        // 将用户角色写入 Sa-Token Session，供 Gateway 鉴权使用
        StpUtil.getSession().set("userRole", userRole);

        return this.getLoginUserVO(user);
    }


    /**
     * 获取当前登录用户
     *
     * @return
     */
    @Override
    public User getLoginUser() {

        // 从数据库查询最新的用户信息
        User currentUser = this.getById(StpUtil.getLoginIdAsLong());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 从 Sa-Token Session 中获取 userId
        Object loginId = StpUtil.getSession().get(USER_LOGIN_STATE);
        if (loginId == null) {
            return null;
        }
        long userId = Long.parseLong(loginId.toString());
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @return
     */
    @Override
    public boolean isAdmin() {
        // 通过 Sa-Token 获取用户角色判断
        String userRole = (String) StpUtil.getSession().get("userRole");
        return UserRoleEnum.ADMIN.getValue().equals(userRole);
    }


    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     *
     */
    @Override
    public boolean userLogout() {

        StpUtil.logout();

        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if(user == null){
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if(user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if(userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = userQueryRequest.getId();

        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id",id);

        queryWrapper.eq(StringUtils.isNotBlank(userRole),"userRole",userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile",userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName",userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    public boolean addUserSignIn(long userId){
        LocalDate date = LocalDate.now();
        String key = RedisConstant.getUserSignInRedisKey(date.getYear(), userId);
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        int offset = date.getDayOfYear();
        if(!signInBitSet.get(offset)){
            //如果未签到，则设置
            signInBitSet.set(offset,true);
        }
        return true;
    }

    public List<Integer> getUserSignInRecord(long userId,Integer year){
        if(year == null){
            LocalDate date = LocalDate.now();
            year = date.getYear();
        }
        String key = RedisConstant.getUserSignInRedisKey(year, userId);
        RBitSet signInBitSet = redissonClient.getBitSet(key);

        //将redis数据缓存到本地
        BitSet bitSet = signInBitSet.asBitSet();
        List<Integer> dayList = new ArrayList<>();

        //从索引0开始查找下一个被设置尾1的位
        int index = bitSet.nextSetBit(0);

        while(index >= 0){
            dayList.add(index);
            index = bitSet.nextSetBit(index+1);
        }

        return dayList;
    }
}
