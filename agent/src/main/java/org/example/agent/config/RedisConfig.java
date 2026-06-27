package org.example.agent.config;


import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;




/**
 * Redis 配置 —— 为 LangChain4j 提供持久化对话记忆存储
 */
@Configuration
public class RedisConfig {

    /**
     * LangChain4j Redis 对话记忆存储
     * <p>
     * 复用 Spring Boot 自动配置的 RedisConnectionFactory，
     * 避免手动创建连接导致的 DefaultedRedisConnection 循环代理问题。
     * <p>
     * 每个会话的对话历史会被持久化到 Redis，key 格式为 "chat-memory:{memoryId}"
     */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(RedisConnectionFactory connectionFactory) {
        return RedisChatMemoryStore.builder()
                .host("localhost")
                .port(6379)
                .ttl(5 * 3600L) // TTL 单位：秒，5 小时
                .build();

    }
}
