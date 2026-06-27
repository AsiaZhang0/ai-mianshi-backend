package org.example.user.service.impl;


import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.example.client.service.InnerUserService;

import org.example.model.vo.UserVO;
import org.example.model.entity.User;
import org.example.user.service.UserService;

import java.util.Collection;
import java.util.Set;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {
    @Resource
    private UserService userService;




    @Override
    public void updateById(User user) {
        userService.updateById(user);
    }

    public User getLoginUser(){
        return userService.getLoginUser();
    }

    public boolean isAdmin(){
        return userService.isAdmin();
    }

    public User getById(long userId){
        return userService.getById(userId);
    }

    public UserVO getUserVO(User user){
        return userService.getUserVO(user);
    }

    public Collection<User> listByIds(Set<Long> userIdSet){
        return userService.listByIds(userIdSet);
    }


}
