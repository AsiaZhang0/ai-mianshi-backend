package org.example.questionbank.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.example.client.service.InnerUserService;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.model.entity.QuestionBankQuestion;
import org.example.model.entity.User;
import org.example.common.response.BaseResponse;
import org.example.common.response.ResultUtils;
import org.example.model.dto.questionBankQuestion.QuestinoBankQuestionBatchAddRequest;
import org.example.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import org.example.model.dto.questionBankQuestion.QuestionBankQuestionRemoveRequest;
import org.example.questionbank.model.vo.QuestionBankQuestionVO;
import org.example.questionbank.service.QuestionBankQuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questionBankQuestion")
@Slf4j
public class QuestionBankQuestionController {

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @DubboReference(check = false)
    private InnerUserService userService;







    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankQuestionVO> getQuestionBankQuestionVOById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        QuestionBankQuestion questionBankQuestion = questionBankQuestionService.getById(id);
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        //获取封装类
        return ResultUtils.success(questionBankQuestionService.getQuestionBankQuestionVO(questionBankQuestion));

    }

    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionBankQuestion>> listQuestionBankQuestionByPage(@RequestBody QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest){
        long current = questionBankQuestionQueryRequest.getCurrent();
        long size = questionBankQuestionQueryRequest.getPageSize();

        //查询数据库
        Page<QuestionBankQuestion> questionBankQuestionPage = questionBankQuestionService.page(new Page<>(current,size),
                questionBankQuestionService.getQueryWrapper(questionBankQuestionQueryRequest));

        return ResultUtils.success(questionBankQuestionPage);
    }


    /**
     * 移除题库题目关联（仅管理员可用）
     * @param questionBankQuestionRemoveRequest
     * @return
     */
    @PostMapping("/remove")
    public BaseResponse<Boolean> removeQuestionBankQuestion(
            @RequestBody QuestionBankQuestionRemoveRequest questionBankQuestionRemoveRequest
    ){
        //参数校验
        ThrowUtils.throwIf(questionBankQuestionRemoveRequest == null, ErrorCode.PARAMS_ERROR);
        Long questionBankId = questionBankQuestionRemoveRequest.getQuestionBankId();
        Long questionId = questionBankQuestionRemoveRequest.getQuestionId();
        ThrowUtils.throwIf(questionBankId == null || questionId == null, ErrorCode.PARAMS_ERROR);

        //构造查询
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .eq(QuestionBankQuestion::getQuestionId,questionId);
        boolean result = questionBankQuestionService.remove(lambdaQueryWrapper);

        return ResultUtils.success(result);
    }

    @PostMapping("/remove/batch")
    public BaseResponse<Boolean> batchRemoveQuestionsFromBank(
            @RequestBody org.example.model.dto.questionBankQuestion.QuestionBankQuestionBatchRemoveRequest questionBankQuestionBatchRemoveRequest
    ) {
        // 参数校验
        ThrowUtils.throwIf(questionBankQuestionBatchRemoveRequest == null, ErrorCode.PARAMS_ERROR);
        Long questionBankId = questionBankQuestionBatchRemoveRequest.getQuestionBankId();
        List<Long> questionIdList = questionBankQuestionBatchRemoveRequest.getQuestionIdList();
        questionBankQuestionService.batchRemoveQuestionsFromBank(questionIdList, questionBankId);
        return ResultUtils.success(true);
    }



    @PostMapping("/add/batch")
    public BaseResponse<Boolean> batchAddQuestionsToBank(@RequestBody QuestinoBankQuestionBatchAddRequest questionBankQuestionBatchAddRequest, HttpServletRequest request){
        ThrowUtils.throwIf(questionBankQuestionBatchAddRequest == null, ErrorCode.PARAMS_ERROR);
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);
        Long questionBankId = questionBankQuestionBatchAddRequest.getQuestionBankId();
        List<Long> questionIdList = questionBankQuestionBatchAddRequest.getQuestionIdList();
        questionBankQuestionService.batchAddQuestionsToBank(questionIdList, questionBankId, loginUser);
        return ResultUtils.success(true);
    }


}