package org.example.questions.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.model.dto.question.QuestionMessage;
import org.example.model.entity.Question;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 题目变更消息生产者 —— 将题目的增删改事件发送到 Kafka
 * <p>
 * 逐条发送，由 Kafka producer 的 batch.size 自动攒批后一次网络请求发出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionEventProducer {

    private final KafkaTemplate<String, QuestionMessage> kafkaTemplate;

    private static final String TOPIC = "question-change";

    /**
     * 全量同步开始 —— 通知 agent 准备接收批量数据
     */
    public void sendFullSyncStart() {
        QuestionMessage message = QuestionMessage.fullSyncStart();
        kafkaTemplate.send(TOPIC, "full-sync-start", message)
                .thenAccept(result -> log.info("Kafka 发送全量同步开始消息"))
                .exceptionally(ex -> {
                    log.error("Kafka 发送全量同步开始消息失败", ex);
                    return null;
                });
    }

    /**
     * 题目新增
     */
    public void sendQuestionCreated(Question question) {
        QuestionMessage message = QuestionMessage.create(question);
        kafkaTemplate.send(TOPIC, String.valueOf(question.getId()), message)
                .thenAccept(result -> log.info("Kafka 发送题目创建消息: questionId={}", question.getId()))
                .exceptionally(ex -> {
                    log.error("Kafka 发送题目创建消息失败: questionId={}", question.getId(), ex);
                    return null;
                });
    }

    /**
     * 题目更新
     */
    public void sendQuestionUpdated(Question question) {
        QuestionMessage message = QuestionMessage.update(question);
        kafkaTemplate.send(TOPIC, String.valueOf(question.getId()), message)
                .thenAccept(result -> log.info("Kafka 发送题目更新消息: questionId={}", question.getId()))
                .exceptionally(ex -> {
                    log.error("Kafka 发送题目更新消息失败: questionId={}", question.getId(), ex);
                    return null;
                });
    }

    /**
     * 题目删除
     */
    public void sendQuestionDeleted(Long questionId) {
        QuestionMessage message = QuestionMessage.delete(questionId);
        kafkaTemplate.send(TOPIC, String.valueOf(questionId), message)
                .thenAccept(result -> log.info("Kafka 发送题目删除消息: questionId={}", questionId))
                .exceptionally(ex -> {
                    log.error("Kafka 发送题目删除消息失败: questionId={}", questionId, ex);
                    return null;
                });
    }
}
