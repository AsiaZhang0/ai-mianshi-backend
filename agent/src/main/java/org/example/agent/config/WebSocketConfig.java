package org.example.agent.config;

import org.example.agent.websocket.BaiduAsrWebSocketHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置
 * 注册 /asr 端点用于百度实时语音识别中转
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BaiduAsrConfig baiduAsrConfig;

    public WebSocketConfig(BaiduAsrConfig baiduAsrConfig) {
        this.baiduAsrConfig = baiduAsrConfig;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new BaiduAsrWebSocketHandler(baiduAsrConfig), "/asr")
                .setAllowedOrigins("*");
    }

    /**
     * 增大 WebSocket 缓冲区，避免大消息被截断
     * 默认文本缓冲区 8KB，增大到 64KB
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);      // 64KB
        container.setMaxBinaryMessageBufferSize(64 * 1024);    // 64KB
        container.setMaxSessionIdleTimeout(5 * 60 * 1000L);    // 5 分钟空闲超时
        return container;
    }
}
