package org.example.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.entity.Question;

import java.io.Serializable;

/**
 * Kafka 题目消息 —— questions 模块发送给 agent 模块
 * <p>
 * 操作类型：FULL_SYNC_START / CREATE / UPDATE / DELETE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型：FULL_SYNC_START / CREATE / UPDATE / DELETE
     */
    private String operation;

    /**
     * 题目 ID
     */
    private Long questionId;

    /**
     * 标题
     */
    private String title;

    /**
     * 难度等级（1=简单, 2=中等, 3=困难）
     */
    private Integer difficultyLevel;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签（JSON 数组字符串）
     */
    private String tags;

    /**
     * 推荐答案
     */
    private String answer;

    /**
     * 创建用户 ID
     */
    private Long userId;

    public static QuestionMessage fullSyncStart() {
        return QuestionMessage.builder()
                .operation("FULL_SYNC_START")
                .build();
    }

    public static QuestionMessage create(Question question) {
        return QuestionMessage.builder()
                .operation("CREATE")
                .questionId(question.getId())
                .title(question.getTitle())
                .difficultyLevel(question.getDifficultyLevel())
                .content(question.getContent())
                .tags(question.getTags())
                .answer(question.getAnswer())
                .userId(question.getUserId())
                .build();
    }

    public static QuestionMessage update(Question question) {
        return QuestionMessage.builder()
                .operation("UPDATE")
                .questionId(question.getId())
                .title(question.getTitle())
                .difficultyLevel(question.getDifficultyLevel())
                .content(question.getContent())
                .tags(question.getTags())
                .answer(question.getAnswer())
                .userId(question.getUserId())
                .build();
    }

    public static QuestionMessage delete(Long questionId) {
        return QuestionMessage.builder()
                .operation("DELETE")
                .questionId(questionId)
                .build();
    }
}
