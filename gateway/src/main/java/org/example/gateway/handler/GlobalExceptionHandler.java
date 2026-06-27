package org.example.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.exception.GatewayErrorCode;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 全局异常处理器（WebFlux 体系）
 * 继承 AbstractErrorWebExceptionHandler 覆盖默认的 DefaultErrorWebExceptionHandler
 */
@Slf4j
@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  WebProperties webProperties,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageReaders(serverCodecConfigurer.getReaders());
        this.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), request -> {
            Throwable ex = getError(request);
            return handleError(ex, request);
        });
    }

    private Mono<ServerResponse> handleError(Throwable ex, ServerRequest request) {
        // 忽略 favicon.ico 等静态资源找不到的异常
        if (ex instanceof NoResourceFoundException) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).build();
        }

        log.error("Gateway 全局异常: {}", ex.getMessage(), ex);

        Map<String, Object> result = new HashMap<>();
        result.put("code", GatewayErrorCode.SYSTEM_ERROR.getCode());
        result.put("data", null);
        result.put("message", GatewayErrorCode.SYSTEM_ERROR.getMessage());

        try {
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"code\":" + GatewayErrorCode.SYSTEM_ERROR.getCode()
                            + ",\"data\":null,\"message\":\"" + GatewayErrorCode.SYSTEM_ERROR.getMessage() + "\"}");
        }
    }
}
