package org.example.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.exception.GatewayErrorCode;
import org.example.gateway.exception.GatewayException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Sa-Token 网关鉴权配置
 * 统一在此处配置哪些路径需要登录、哪些路径需要角色
 * <p>
 * 角色说明：user（普通用户）、admin（管理员）、ban（封号）
 * 普通用户只能访问查询类接口，管理员可以访问管理类接口
 */
@Slf4j
@Configuration
public class SaTokenConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                // 拦截所有 /api/** 的请求
                .addInclude("/api/**")
                // 排除登录、注册等无需鉴权的路径
                .addExclude(
                        // ========== 公开接口 ==========
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/logout",
                        "/api/questionBank/list/page",
                        "/api/questions/list/page/vo",
                        // Knife4j 文档相关路径
                        "/doc.html",
                        "/api/user/v3/api-docs",
                        "/api/questions/v3/api-docs",
                        "/api/questionBank/v3/api-docs",
                        "/api/agent/v3/api-docs",
                        "/api/file/v3/api-docs",
                        "/webjars/**",
                        "/favicon.ico"
                )
                // CORS 预检请求放行：OPTIONS 请求不需要鉴权
                .setBeforeAuth(r -> {
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        // 直接标记为放行，跳过后续鉴权
                        SaRouter.stop();
                    }
                })
                // 鉴权规则
                .setAuth(obj -> {
                    // 调试日志：打印当前请求路径和 Token 信息
                    String requestPath = SaHolder.getRequest().getRequestPath();
                    String token = SaHolder.getRequest().getHeader("satoken");
                    boolean isLogin = StpUtil.isLogin();
                    Object loginId = isLogin ? StpUtil.getLoginIdDefaultNull() : null;
                    log.debug("[Sa-Token 鉴权] 路径: {}, Token: {}, 已登录: {}, LoginId: {}",
                            requestPath, token, isLogin, loginId);

                    // ==========================================
                    // 一、管理员接口（admin 角色）
                    // ==========================================
                    // -- User 模块 --
                    SaRouter.match("/api/user/add", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/user/delete", r -> StpUtil.checkRole("admin"));
                    //SaRouter.match("/api/user/update", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/user/get", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/user/list/page", r -> StpUtil.checkRole("admin"));

                    // -- Questions 模块 --
                    SaRouter.match("/api/questions/add", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/questions/update", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/questions/list/page", r -> StpUtil.checkRole("admin"));
                    // 注意：/api/questions/edit 和 /api/questions/delete 不在此处校验角色，
                    // "仅本人或管理员"的逻辑在 QuestionController 中实现

                    // -- QuestionBank 模块 --
                    SaRouter.match("/api/questionBank/add", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/questionBank/update", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/questionBank/list/page", r -> StpUtil.checkRole("admin"));
                    // 注意：/api/questionBank/edit 和 /api/questionBank/delete 不在此处校验角色，
                    // "仅本人或管理员"的逻辑在 QuestionBankController 中实现

                    // ==========================================
                    // 二、其余 /api/** 接口只需登录即可（普通用户 + 管理员均可）
                    // ban 用户通过 GatewayException(BANNED) 在 StpInterfaceImpl 中拦截
                    // ==========================================
                    SaRouter.match("/api/**", r -> StpUtil.checkLogin());
                })
                // 异常处理：统一转为 GatewayException 后返回 JSON
                .setError(e -> {
                    // 调试日志：打印鉴权失败的详细信息
                    String requestPath = SaHolder.getRequest().getRequestPath();
                    String token = SaHolder.getRequest().getHeader("satoken");
                    log.warn("[Sa-Token 鉴权失败] 路径: {}, Token: {}, 异常: {}",
                            requestPath, token, e.getMessage());
                    GatewayException ge = toGatewayException(e);
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", ge.getCode());
                    result.put("data", null);
                    result.put("message", ge.getMessage());
                    try {
                        return objectMapper.writeValueAsString(result);
                    } catch (Exception ex) {
                        return "{\"code\":" + ge.getCode() + ",\"data\":null,\"message\":\"" + ge.getMessage() + "\"}";
                    }
                });
    }

    private GatewayException toGatewayException(Throwable e) {
        if (e instanceof GatewayException) {
            return (GatewayException) e;
        }
        if (e instanceof cn.dev33.satoken.exception.NotLoginException) {
            return new GatewayException(GatewayErrorCode.NOT_LOGIN);
        }
        if (e instanceof cn.dev33.satoken.exception.NotRoleException) {
            return new GatewayException(GatewayErrorCode.NO_AUTH,
                    "缺少角色 " + ((cn.dev33.satoken.exception.NotRoleException) e).getRole());
        }
        if (e instanceof cn.dev33.satoken.exception.NotPermissionException) {
            return new GatewayException(GatewayErrorCode.NO_AUTH);
        }
        return new GatewayException(GatewayErrorCode.SYSTEM_ERROR,
                e.getMessage() != null ? e.getMessage() : GatewayErrorCode.SYSTEM_ERROR.getMessage());
    }
}
