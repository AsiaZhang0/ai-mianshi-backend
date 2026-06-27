package org.example.client.service;



import org.example.model.entity.QuestionBankQuestion;

import java.util.List;

public interface InnerQuestionBankQuestionService {

    /**
     * 根据题库ID查询题目关联列表
     */
    List<QuestionBankQuestion> listByQuestionBankId(Long questionBankId);

    /**
     * 根据题目ID移除题目题库关联
     */
    boolean removeByQuestionId(Long questionId);
}
