package org.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.model.entity.Question;

/**
 * 仅在 questionBank 模块中注册 Question 实体的 MyBatis-Plus Lambda 缓存，
 * 以便使用 LambdaQueryWrapper<Question> 进行 Dubbo RPC 序列化。
 * 实际查询由 Dubbo 远程调用 questions 服务完成。
 */
public interface QuestionMapper extends BaseMapper<Question> {
}
