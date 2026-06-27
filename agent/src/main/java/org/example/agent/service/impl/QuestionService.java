package org.example.agent.service.impl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.mapper.EmbeddingVectorMapper;
import org.example.model.entity.Question;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QuestionService {

    @Resource
    EmbeddingModel embeddingModel;

    @Resource
    EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    EmbeddingVectorMapper embeddingVectorMapper;

    /**
     * 将题目向量化并存入 pgvector，附带元数据（question_id、difficulty_level）
     * <p>
     * 先通过 MyBatis-Plus 查 question_id 是否已在向量表中，已存在则跳过。
     *
     * @param question 题目实体
     */
    public void embedAndStore(Question question) {
        if (isQuestionEmbedded(question.getId())) {
            log.info("题目 [id={}] 向量已存在，跳过", question.getId());
            return;
        }

        String text = buildEmbeddingText(question);
        Embedding embedding = embeddingModel.embed(text).content();

        Metadata metadata = Metadata.from(
                Map.of("question_id", question.getId(),
                        "difficulty_level", question.getDifficultyLevel() != null ? question.getDifficultyLevel() : 0));
        TextSegment segment = TextSegment.from(text, metadata);
        embeddingStore.add(embedding, segment);
        log.info("题目 [id={}, title={}] 向量化并存储成功", question.getId(), question.getTitle());
    }

    /**
     * 批量向量化并存储题目（过滤已存在的，对新题目一次性调用 embedAll 生成向量）
     */
    public void embedAndStoreBatch(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        // 过滤掉已存在的
        List<Question> newQuestions = questions.stream()
                .filter(q -> !isQuestionEmbedded(q.getId()))
                .toList();

        if (newQuestions.isEmpty()) {
            log.info("批量向量化: 所有 {} 道题目已存在，跳过", questions.size());
            return;
        }

        log.info("批量向量化: 总共 {} 道，新题目 {} 道，已存在 {} 道",
                questions.size(), newQuestions.size(), questions.size() - newQuestions.size());

        // 构建 TextSegment 列表
        List<TextSegment> segments = newQuestions.stream()
                .map(q -> {
                    String text = buildEmbeddingText(q);
                    Metadata metadata = Metadata.from(
                            Map.of("question_id", q.getId(),
                                    "difficulty_level", q.getDifficultyLevel() != null ? q.getDifficultyLevel() : -1));
                    return TextSegment.from(text, metadata);
                })
                .toList();

        // API 限制每批最多 10 条，分批生成嵌入向量
        int batchSize = 10;
        for (int start = 0; start < segments.size(); start += batchSize) {
            int end = Math.min(start + batchSize, segments.size());
            List<TextSegment> batchSegments = segments.subList(start, end);
            List<Embedding> batchEmbeddings = embeddingModel.embedAll(batchSegments).content();
            for (int i = 0; i < batchSegments.size(); i++) {
                embeddingStore.add(batchEmbeddings.get(i), batchSegments.get(i));
            }
        }

        log.info("批量向量化并存储 {} 道新题目成功", newQuestions.size());
    }

    /**
     * 判断指定 question_id 的向量是否已在向量库中（通过 MyBatis-Plus 查询）
     */
    public boolean isQuestionEmbedded(Long questionId) {
        return embeddingVectorMapper.existsByQuestionId(questionId);
    }

    /**
     * RAG 语义搜索：根据用户输入检索最相关的题目
     */
    public List<TextSegment> searchRelevant(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        log.info("RAG 检索: query='{}', 命中 {} 条结果", query, matches.size());
        return matches.stream().map(EmbeddingMatch::embedded).toList();
    }

    /**
     * RAG 语义搜索：按难度过滤
     */
    public List<TextSegment> searchRelevantByDifficulty(String query, int difficultyLevel, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        Filter filter = new IsEqualTo("difficulty_level", difficultyLevel);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.5)
                .filter(filter)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        log.info("RAG 检索(难度={}): query='{}', 命中 {} 条结果", difficultyLevel, query, matches.size());
        return matches.stream().map(EmbeddingMatch::embedded).toList();
    }

    /**
     * 清空所有题目向量数据
     */
    public void clearAllEmbeddings() {
        try {
            embeddingStore.removeAll();
            log.info("已清空所有题目向量数据");
        } catch (Exception e) {
            log.warn("清空向量数据时出现异常（可能是表为空）: {}", e.getMessage());
        }
    }

    /**
     * 删除指定题目的嵌入向量（通过 question_id 直接删除，不走向量搜索）
     */
    public void deleteEmbedding(Long questionId) {
        embeddingVectorMapper.deleteByQuestionId(questionId);
        log.info("题目 [id={}] 的向量记录已删除", questionId);
    }

    /**
     * 构建用于向量化的文本：标题 + 内容
     */
    private String buildEmbeddingText(Question question) {
        StringBuilder sb = new StringBuilder();
        if (question.getTitle() != null && !question.getTitle().isBlank()) {
            sb.append("题目：").append(question.getTitle());
        }
        if (question.getContent() != null && !question.getContent().isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("内容：").append(question.getContent());
        }
        return sb.toString();
    }
}
