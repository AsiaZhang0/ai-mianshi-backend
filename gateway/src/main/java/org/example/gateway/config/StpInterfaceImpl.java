package org.example.gateway.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.example.gateway.exception.GatewayException;
import org.example.gateway.exception.GatewayErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限角色注入实现
 * 从 Redis Session 中读取用户角色，供 Gateway 鉴权使用
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 获取用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        if (session == null) {
            return Collections.emptyList();
        }
        String userRole = (String) session.get("userRole");
        if (userRole == null) {
            return Collections.emptyList();
        }
        // ban 用户直接抛异常，由 SaReactorFilter.setError() 统一返回前端
        if ("ban".equals(userRole)) {
            throw new GatewayException(GatewayErrorCode.BANNED);
        }
        return Collections.singletonList(userRole);
    }

    /**
     * 获取用户权限列表（暂不细分权限，只用角色控制）
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
