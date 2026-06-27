package org.example.agent.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.service.impl.QuestionService;
import org.example.model.entity.Question;
import org.example.model.dto.question.QuestionMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目变更消息消费者 —— 接收 questions 模块的增删改事件，同步更新 pgvector
 * <p>
 * - FULL_SYNC_START: 标记全量同步开始，不执行清空操作
 * - CREATE: 收集本批所有 CREATE 消息，批量生成嵌入向量并插入
 * - UPDATE: 先删除旧向量，再重新生成并插入
 * - DELETE: 删除对应的向量记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionEventConsumer {

    private final QuestionService questionService;

    private static final String TOPIC = "question-change";
    private static final String GROUP_ID = "agent-question-consumer";

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID, batch = "true")
    public void onQuestionEvent(List<QuestionMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        log.info("收到批量消息: {} 条", messages.size());

        // 收集本批所有 CREATE 的题目，最后统一批量生成向量
        List<Question> createQuestions = new ArrayList<>();

        for (QuestionMessage message : messages) {
            if (message == null) {
                continue;
            }

            String operation = message.getOperation();
            Long questionId = message.getQuestionId();

            try {
                switch (operation) {
                    case "FULL_SYNC_START" -> {
                        log.info("全量同步开始标记，不清空向量库");
                    }
                    case "CREATE" -> {
                        createQuestions.add(toEntity(message));
                    }
                    case "UPDATE" -> {
                        Question question = toEntity(message);
                        questionService.deleteEmbedding(questionId);
                        questionService.embedAndStore(question);
                        log.info("题目向量 UPDATE 成功: questionId={}", questionId);
                    }
                    case "DELETE" -> {
                        questionService.deleteEmbedding(questionId);
                        log.info("题目向量删除成功: questionId={}", questionId);
                    }
                    default -> log.warn("未知的题目操作类型: {}", operation);
                }
            } catch (Exception e) {
                log.error("处理题目变更消息失败: questionId={}, operation={}", questionId, operation, e);
            }
        }

        // 批量生成嵌入向量
        if (!createQuestions.isEmpty()) {
            questionService.embedAndStoreBatch(createQuestions);
        }
    }

    private Question toEntity(QuestionMessage message) {
        Question question = new Question();
        question.setId(message.getQuestionId());
        question.setTitle(message.getTitle());
        question.setDifficultyLevel(message.getDifficultyLevel());
        question.setContent(message.getContent());
        question.setTags(message.getTags());
        question.setAnswer(message.getAnswer());
        question.setUserId(message.getUserId());
        return question;
    }
}
