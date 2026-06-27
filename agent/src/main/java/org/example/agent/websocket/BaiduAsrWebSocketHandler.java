package org.example.agent.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.config.BaiduAsrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 百度实时语音识别 WebSocket 处理器
 * <p>
 * 文档: https://cloud.baidu.com/doc/SPEECH/s/jlbxejt2i
 * <p>
 * 流程：
 * 1. 前端通过 WebSocket 连接后端 /asr 端点
 * 2. 后端收到 "START" 文本帧后，建立到百度 ASR 的 WebSocket 连接
 * 3. 后端向百度发送 START 帧（含 appid/appkey 鉴权）
 * 4. 前端发送音频二进制帧 → 后端透传给百度 ASR
 * 5. 百度返回识别结果（MID_TEXT 临时 / FIN_TEXT 最终）→ 后端转发给前端
 * 6. 前端发送 "FINISH" → 后端发送 FINISH 帧并关闭百度连接
 */
public class BaiduAsrWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BaiduAsrWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final BaiduAsrConfig config;

    /** 前端 WebSocket Session ID -> 百度 WebSocketSession 映射 */
    private final Map<String, WebSocketSession> baiduSessions = new ConcurrentHashMap<>();

    /** 前端 Session ID -> 心跳保活任务 */
    private final Map<String, ScheduledFuture<?>> keepAliveTasks = new ConcurrentHashMap<>();

    /** 心跳定时线程池 */
    private final ScheduledExecutorService keepAliveExecutor = Executors.newScheduledThreadPool(4);

    /** 静音帧：160ms 的零值 PCM 数据 (16000 * 2 * 160 / 1000 = 5120 bytes) */
    private static final byte[] SILENCE_FRAME = new byte[5120];

    /** StandardWebSocketClient 复用 */
    private final StandardWebSocketClient wsClient = new StandardWebSocketClient();

    public BaiduAsrWebSocketHandler(BaiduAsrConfig config) {
        this.config = config;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[ASR] 前端 WebSocket 已连接: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMsg) {
            handleTextMessage(session, textMsg.getPayload());
        } else if (message instanceof BinaryMessage binaryMsg) {
            handleBinaryMessage(session, binaryMsg.getPayload());
        }
    }

    /**
     * 处理文本帧（控制命令）
     */
    private void handleTextMessage(WebSocketSession session, String payload) throws Exception {
        log.info("[ASR] 收到文本帧: {}", payload);

        JsonNode json;
        try {
            json = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("[ASR] 无法解析 JSON: {}", payload);
            return;
        }

        String type = json.has("type") ? json.get("type").asText() : "";

        switch (type) {
            case "START" -> {
                log.info("[ASR] 收到 START 帧，开始连接百度 ASR");
                connectToBaidu(session);
            }
            case "FINISH" -> {
                log.info("[ASR] 收到 FINISH 帧，关闭百度连接");
                closeBaiduConnection(session);
            }
            default -> log.warn("[ASR] 未知帧类型: {}", type);
        }
    }

    /**
     * 处理二进制帧（音频数据）→ 透传给百度 ASR
     * <p>
     * 百度要求: PCM 16kHz 16bits，建议每帧 5120 bytes (160ms)
     */
    private void handleBinaryMessage(WebSocketSession session, ByteBuffer payload) {
        log.debug("[ASR] 收到二进制音频帧, session={}, size={} bytes", session.getId(), payload.remaining());
        WebSocketSession baiduSession = baiduSessions.get(session.getId());
        if (baiduSession != null && baiduSession.isOpen()) {
            try {
                baiduSession.sendMessage(new BinaryMessage(payload));
            } catch (IOException e) {
                log.error("[ASR] 转发音频帧到百度失败", e);
            }
        }
    }

    /**
     * 建立到百度 ASR 的 WebSocket 连接
     * <p>
     * 连接地址: wss://vop.baidu.com/realtime_asr?sn=xxx
     * sn 为用户自定义标识，用于百度侧排查日志
     * 鉴权通过 START 帧中的 appid/appkey 完成，不需要 OAuth token
     */
    private void connectToBaidu(WebSocketSession frontSession) throws ExecutionException, InterruptedException {
        // sn 为用户自定义标识，用于排查日志
        String sn = UUID.randomUUID().toString().replace("-", "");
        String wsUrl = "wss://vop.baidu.com/realtime_asr?sn=" + sn;
        log.info("[ASR] 连接百度 ASR: sn={}", sn);

        WebSocketSession baiduSession = wsClient.execute(
                new WebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        log.info("[ASR] 百度 WebSocket 已连接: {}", session.getId());

                        // 发送 START 帧到百度（在此帧中完成鉴权）
                        try {
                            String startFrame = objectMapper.writeValueAsString(Map.of(
                                    "type", "START",
                                    "data", Map.of(
                                            "appid", config.getAppId(),
                                            "appkey", config.getApiKey(),
                                            "dev_pid", config.getDevPid(),
                                            "cuid", String.valueOf(config.getAppId()),
                                            "format", "pcm",
                                            "sample", 16000
                                    )
                            ));
                            session.sendMessage(new TextMessage(startFrame));
                            log.info("[ASR] 已发送 START 帧到百度");
                        } catch (Exception e) {
                            log.error("[ASR] 发送 START 帧失败", e);
                        }
                    }

                    @Override
                    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                        //log.info("[ASR] 收到百度 WebSocket 消息: {}", message);
                        if (message instanceof TextMessage textMsg) {
                            String msg = textMsg.getPayload();
                            log.info("[ASR] 百度返回: {}", msg);
                            // 解析百度响应，记录类型
                            try {
                                JsonNode resp = objectMapper.readTree(msg);
                                String respType = resp.has("type") ? resp.get("type").asText() : "";
                                // 心跳帧忽略，不记录错误
                                if ("HEARTBEAT".equals(respType)) {
                                    // skip
                                } else {
                                    int errNo = resp.has("err_no") ? resp.get("err_no").asInt() : -1;
                                    if (errNo != 0) {
                                        log.warn("[ASR] 百度返回错误: err_no={}, err_msg={}",
                                                errNo, resp.has("err_msg") ? resp.get("err_msg").asText() : "");
                                    } else {
                                        String result = resp.has("result") ? resp.get("result").asText() : "";
                                        log.info("[ASR] 百度识别结果[{}]: {}", respType, result);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        // 转发给前端
                        try {
                            if (frontSession.isOpen()) {

                                frontSession.sendMessage(message);
                            }
                        } catch (IOException e) {
                            log.error("[ASR] 转发百度响应到前端失败", e);
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable ex) {
                        log.error("[ASR] 百度 WebSocket 传输错误", ex);
                        baiduSessions.remove(frontSession.getId());
                        try {
                            if (frontSession.isOpen()) {
                                frontSession.sendMessage(new TextMessage(
                                        objectMapper.writeValueAsString(Map.of(
                                                "type", "ERROR",
                                                "error", Map.of("msg", "百度 ASR 连接异常: " + ex.getMessage())
                                        ))
                                ));
                            }
                        } catch (IOException ignored) {}
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                        log.info("[ASR] 百度 WebSocket 已关闭: {}", status);
                        baiduSessions.remove(frontSession.getId());
                    }

                    @Override
                    public boolean supportsPartialMessages() {
                        return false;
                    }
                },
                wsUrl
        ).get();

        baiduSessions.put(frontSession.getId(), baiduSession);

        // 启动心跳保活：每 3 秒发送静音帧，防止百度 5 秒超时断开
        ScheduledFuture<?> keepAlive = keepAliveExecutor.scheduleWithFixedDelay(() -> {
            WebSocketSession bs = baiduSessions.get(frontSession.getId());
            if (bs != null && bs.isOpen()) {
                try {
                    bs.sendMessage(new BinaryMessage(ByteBuffer.wrap(SILENCE_FRAME)));
                    log.debug("[ASR] 发送静音保活帧");
                } catch (IOException e) {
                    log.warn("[ASR] 保活帧发送失败", e);
                }
            }
        }, 3, 3, TimeUnit.SECONDS);
        keepAliveTasks.put(frontSession.getId(), keepAlive);

        log.info("[ASR] 百度 ASR 连接建立完成");
    }

    /**
     * 关闭百度连接
     */
    private void closeBaiduConnection(WebSocketSession frontSession) {
        // 先取消心跳保活任务
        ScheduledFuture<?> keepAlive = keepAliveTasks.remove(frontSession.getId());
        if (keepAlive != null) {
            keepAlive.cancel(false);
        }

        WebSocketSession baiduSession = baiduSessions.remove(frontSession.getId());
        if (baiduSession != null && baiduSession.isOpen()) {
            try {
                // 发送 FINISH 帧，通知百度识别结束
                baiduSession.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(Map.of("type", "FINISH"))));
                baiduSession.close();
                log.info("[ASR] 已发送 FINISH 帧并关闭百度连接");
            } catch (Exception e) {
                log.error("[ASR] 关闭百度连接失败", e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable ex) {
        log.error("[ASR] 前端 WebSocket 传输错误: {}", session.getId(), ex);
        closeBaiduConnection(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[ASR] 前端 WebSocket 已关闭: {} - {}", session.getId(), status);
        closeBaiduConnection(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
