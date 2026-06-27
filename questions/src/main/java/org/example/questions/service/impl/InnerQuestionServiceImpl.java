package org.example.questions.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.example.client.service.InnerQuestionService;

import org.example.model.dto.question.QuestionQueryRequest;
import org.example.model.entity.Question;
import org.example.model.vo.QuestionVO;
import org.example.questions.service.QuestionService;

import java.util.List;
import java.util.function.Function;


@DubboService
public class InnerQuestionServiceImpl implements InnerQuestionService {
    @Resource
    private QuestionService questionService;

    @Override
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        return questionService.listQuestionByPage(questionQueryRequest);
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage) {
        return questionService.getQuestionVOPage(questionPage);
    }

    @Override
    public List<Long> listObjs(List<Long> questionIdList) {
        LambdaQueryWrapper<Question> lambdaQueryWrapper = Wrappers.lambdaQuery(Question.class).select(
                Question::getId
        ).in(Question::getId, questionIdList);

        return questionService.listObjs(lambdaQueryWrapper,o->(Long) o);
    }

    @Override
    public Question getById(Long questionId) {
        return questionService.getById(questionId);
    }
}
