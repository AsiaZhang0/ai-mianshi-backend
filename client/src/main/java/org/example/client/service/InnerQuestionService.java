package org.example.client.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.model.dto.question.QuestionQueryRequest;
import org.example.model.entity.Question;
import org.example.model.vo.QuestionVO;


import java.util.List;
import java.util.function.Function;

public interface InnerQuestionService {
    Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest);

    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage);

    List<Long> listObjs(List<Long> questionIdList);

    Question getById(Long questionId);

}
