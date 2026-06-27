package org.example.client.service;


import org.example.model.vo.UserVO;
import org.example.model.entity.User;

import java.util.Collection;
import java.util.Set;

public interface InnerUserService {

    void updateById(User user);

    /**
     * @deprecated Dubbo RPC 调用时没有 HTTP 上下文，不能使用 StpUtil，
     *             请使用 getById(userId) 替代
     */
    @Deprecated
    User getLoginUser();

    boolean isAdmin();

    User getById(long userId);

    UserVO getUserVO(User user);

    Collection<User> listByIds(Set<Long> userIdSet);
}
