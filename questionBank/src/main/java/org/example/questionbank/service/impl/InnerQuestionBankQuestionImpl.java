package org.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.example.client.service.InnerQuestionBankQuestionService;
import org.example.questionbank.service.QuestionBankQuestionService;
import org.example.model.entity.QuestionBankQuestion;
import java.util.List;


@DubboService
public class InnerQuestionBankQuestionImpl implements InnerQuestionBankQuestionService {

    @Resource
    QuestionBankQuestionService questionBankQuestionService;

    @Override
    public List<QuestionBankQuestion> listByQuestionBankId(Long questionBankId) {
        LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(QuestionBankQuestion::getQuestionId)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
        return questionBankQuestionService.list(wrapper);
    }

    @Override
    public boolean removeByQuestionId(Long questionId) {
        LambdaQueryWrapper<QuestionBankQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBankQuestion::getQuestionId, questionId);
        return questionBankQuestionService.remove(wrapper);
    }
}
