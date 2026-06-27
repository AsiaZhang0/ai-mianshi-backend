package org.example.agent.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 映射 LangChain4j PgVectorEmbeddingStore 创建的向量表 ai_mianshi.questions
 * <p>
 * COLUMN_PER_KEY 模式下，question_id 和 difficulty_level 为独立 metadata 列。
 * embedding 列为 pgvector vector 类型，通过 TypeHandler 处理。
 */
@Data
@TableName(value = "ai_mianshi.questions", autoResultMap = true)
public class EmbeddingVector implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 向量记录唯一 ID（UUID，由 LangChain4j 生成）
     */
    @TableId
    private String embeddingId;

    /**
     * 向量数据（pgvector vector(1024) 类型）
     */
    @TableField(typeHandler = org.example.agent.config.typehandler.PgVectorTypeHandler.class)
    private float[] embedding;

    /**
     * 向量化之前的原始文本内容
     */
    private String text;

    /**
     * 题目 ID（metadata 列，UNIQUE 约束）
     */
    private Long questionId;

    /**
     * 难度等级（metadata 列）
     */
    private String difficultyLevel;
}

