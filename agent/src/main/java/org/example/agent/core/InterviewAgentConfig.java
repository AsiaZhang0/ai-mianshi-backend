package org.example.agent.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.core.tools.MemoryTool;
import org.example.agent.core.service.InterviewAgentService;
import org.example.agent.core.tools.LogTool;
import org.example.agent.core.tools.RagTool;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 面试 Agent 配置 —— 使用 @Agent 注解 + AgenticServices 构建
 * <p>
 * 核心机制：
 * 1. 使用 @Agent 注解声明智能体，@SystemMessage 定义系统提示
 * 2. 将 RagTool 注册为工具，Agent 在对话中自主决定调用时机
 * 3. 使用 RedisChatMemoryStore 持久化对话记忆，服务重启不丢失
 * 4. 通过 MessageWindowChatMemory 限制记忆窗口大小（最近 20 条消息）
 * 5. 使用 Caffeine 本地缓存 Agent 实例，避免每次请求重复构建
 * <p>
 * 流式输出：Agent 方法返回 Flux&lt;String&gt;，构建时使用 StreamingChatModel
 */
@Slf4j
@AllArgsConstructor
@Configuration
public class InterviewAgentConfig {

    private final StreamingChatModel streamingChatModel;
    private final RagTool ragTool;
    private final LogTool logTool;
    private final MemoryTool memoryTool;
    private final RedisChatMemoryStore redisChatMemoryStore;

    /**
     * Caffeine 本地缓存：memoryId → InterviewAgentService
     * <p>
     * - 最大 1000 个会话
     * - 30 分钟未访问自动过期（面试对话间隔通常不会超过 30 分钟）
     * - 过期后自动清理，下次请求重新构建（ChatMemory 从 Redis 恢复，不丢上下文）
     */
    private final Cache<String, InterviewAgentService> agentCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((String key, InterviewAgentService value, RemovalCause cause) -> {
                log.info("Agent 缓存移除: memoryId={}, 原因={}", key, cause);
            })
            .build();

    /**
     * 获取或创建 Agent 实例（带 Caffeine 缓存）
     * <p>
     * 首次请求时构建 Agent 并缓存，后续请求直接从缓存获取。
     * ChatMemory 由 RedisChatMemoryStore 持久化，缓存过期后重建也不丢失上下文。
     */
    public InterviewAgentService getOrCreateAgent(String memoryId) {
        return agentCache.get(memoryId, id -> {
            log.info("构建新 Agent 实例: memoryId={}", id);
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(20)
                    .chatMemoryStore(redisChatMemoryStore)
                    .build();

            return AgenticServices.agentBuilder(InterviewAgentService.class)
                    .streamingChatModel(streamingChatModel)
                    .tools(ragTool, logTool, memoryTool)
                    .chatMemory(chatMemory)
                    .build();
        });
    }
}
