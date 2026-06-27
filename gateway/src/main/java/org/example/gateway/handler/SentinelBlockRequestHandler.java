package org.example.gateway.handler;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sentinel Gateway 自定义限流/熔断响应处理器
 * <p>
 * 统一处理所有 BlockException（FlowException、ParamFlowException、DegradeException 等），
 * 替代 application.yaml 中的 scg.fallback 静态配置，
 * 确保无论触发哪种限流/熔断规则，返回的响应格式都一致。
 */
@Slf4j
@Component
public class SentinelBlockRequestHandler implements BlockRequestHandler {

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
        log.warn("Sentinel 限流/熔断触发: {}", t.getMessage());

        // 统一返回 500 + 自定义 JSON（而不是 Sentinel 默认的 429）
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"code\":500,\"message\":\"服务器繁忙，请稍后再试\"}");
    }
}
