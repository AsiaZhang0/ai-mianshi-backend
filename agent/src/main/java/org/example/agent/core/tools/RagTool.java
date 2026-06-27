package org.example.agent.core.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * RAG 知识库检索工具 —— 供 Agent 在对话过程中自动调用
 * <p>
 * LLM 会根据对话上下文自动判断是否需要检索知识库，
 * 并在需要时调用 searchKnowledgeBase / searchKnowledgeBaseByDifficulty。
 */
@Slf4j
@Component
public class RagTool {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 在面试题库中搜索与查询相关的题目。
     * 当需要了解某个技术领域有哪些面试题、或者需要参考题库出题时调用。
     *
     * @param query      搜索关键词或问题描述，例如 "Java 多线程"、"Redis 缓存雪崩"
     * @param maxResults 返回的最大结果数，建议 3~5
     * @return 匹配的题目内容，以文本形式返回
     */
    @Tool("在面试题库中搜索与查询相关的题目。当需要了解某个技术领域有哪些面试题、或者需要参考题库出题时调用此工具")
    public String searchKnowledgeBase(
            @ToolMemoryId String memoryId,
            String query,
            int maxResults) {
        log.info("[RagTool] 对话 {} 检索知识库: query={}, maxResults={}", memoryId, query, maxResults);

        var queryEmbedding = embeddingModel.embed(query).content();
        var request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Math.min(maxResults, 5))
                .minScore(0.4)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        log.info("[RagTool] 检索完成: 命中 {} 条结果", result.matches().size());

        if (result.matches().isEmpty()) {
            return "未找到与「" + query + "」相关的题目";
        }

        return result.matches().stream()
                .map(m -> {
                    TextSegment seg = m.embedded();
                    double score = m.score();
                    return String.format("[相关度: %.2f] %s", score, seg.text());
                })
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 在面试题库中按难度等级搜索题目。
     * 当候选人要求调整难度或需要特定难度的问题时调用。
     *
     * @param query           搜索关键词
     * @param difficultyLevel 难度等级：1=简单, 2=中等, 3=困难
     * @param maxResults      最大返回数
     * @return 匹配的题目内容
     */
    @Tool("按难度等级在面试题库中搜索题目。难度等级：1=简单, 2=中等, 3=困难。当候选人要求调整难度时调用")
    public String searchKnowledgeBaseByDifficulty(
            @ToolMemoryId String memoryId,
            String query,
            int difficultyLevel,
            int maxResults) {
        log.info("[RagTool] 对话 {} 按难度检索: query={}, difficulty={}, maxResults={}",
                memoryId, query, difficultyLevel, maxResults);

        var queryEmbedding = embeddingModel.embed(query).content();
        Filter filter = new IsEqualTo("difficulty_level", difficultyLevel);
        var request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Math.min(maxResults, 5))
                .minScore(0.4)
                .filter(filter)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        log.info("[RagTool] 按难度检索完成: 命中 {} 条结果", result.matches().size());

        if (result.matches().isEmpty()) {
            return "未找到难度为 " + difficultyLevel + " 且与「" + query + "」相关的题目";
        }

        String levelName = switch (difficultyLevel) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "未知";
        };

        return "【难度: " + levelName + "】\n" +
                result.matches().stream()
                        .map(m -> {
                            TextSegment seg = m.embedded();
                            double score = m.score();
                            return String.format("[相关度: %.2f] %s", score, seg.text());
                        })
                        .collect(Collectors.joining("\n---\n"));
    }
}
