package org.example.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent.model.entity.EmbeddingVector;

/**
 * 向量表 Mapper，用于判断 question_id 是否已存在、按 question_id 删除。
 */
@Mapper
public interface EmbeddingVectorMapper extends BaseMapper<EmbeddingVector> {

    /**
     * 检查指定 question_id 是否已在向量表中
     */
    @Select("SELECT COUNT(*) > 0 FROM ai_mianshi.questions WHERE question_id = #{questionId}")
    boolean existsByQuestionId(@Param("questionId") Long questionId);

    /**
     * 按 question_id 删除向量记录
     */
    @Delete("DELETE FROM ai_mianshi.questions WHERE question_id = #{questionId}")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}
