package org.example.questionbank.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.example.client.service.InnerQuestionService;
import org.example.client.service.InnerUserService;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.model.dto.question.QuestionQueryRequest;
import org.example.model.entity.Question;
import org.example.model.entity.User;
import org.example.model.vo.QuestionVO;
import org.example.common.request.DeleteRequest;
import org.example.common.response.BaseResponse;
import org.example.common.response.ResultUtils;
import org.example.model.dto.questionBank.QuestionBankAddRequest;
import org.example.model.dto.questionBank.QuestionBankEditRequest;
import org.example.model.dto.questionBank.QuestionBankQueryRequest;
import org.example.model.dto.questionBank.QuestionBankUpdateRequest;
import org.example.model.entity.QuestionBank;
import org.example.questionbank.model.vo.QuestionBankVO;
import org.example.questionbank.service.QuestionBankService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @DubboReference(check = false)
    private InnerQuestionService questionService;

    @DubboReference(check = false)
    private InnerUserService userService;

    //region 增删改查
    @PostMapping("/add")
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // todo 填充默认值
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long loginUserId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(loginUserId);
        long id = deleteRequest.getId();

        //判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);

        //仅本人或管理员可删除
        if(!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest){

        if(questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //todo 在此处将实体类和DTO进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);

        //数据校验
        questionBankService.validQuestionBank(questionBank, false);
        //判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }


    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest){
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 生成 key
        String key = "bank_detail_" + id;
        // 如果是热 key
//        if (JdHotKeyStore.isHotKey(key)) {
//            // 从本地缓存中获取缓存值
//            Object cachedQuestionBankVO = JdHotKeyStore.get(key);
//            if (cachedQuestionBankVO != null) {
//                // 如果缓存中有值，直接返回缓存的值
//                return ResultUtils.success((QuestionBankVO) cachedQuestionBankVO);
//            }
//        }

        //查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);

        //查询题库封装类
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank);
        //是否要关联查询题库下的题目列表
        boolean needQueryQuestionList = questionBankQueryRequest.isNeedQueryQuesitionList();

        if(needQueryQuestionList){
            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            Page<QuestionVO> questionVOPage = questionService.getQuestionVOPage(questionPage);
            questionBankVO.setQuestionPage(questionVOPage);
        }

//        JdHotKeyStore.smartSet(key, questionBankVO);
        //获取封装类
        return ResultUtils.success(questionBankVO);
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest){
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        //限制ai爬虫,第一页不限制
        ThrowUtils.throwIf((current != 1 || size > 30) && !StpUtil.isLogin(), ErrorCode.NO_AUTH_ERROR);
        //查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current,size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));

        return ResultUtils.success(questionBankPage);
    }

//    @PostMapping("/list/firstPage")
//    public BaseResponse<Page<QuestionBank>> listQuestionBankByFirstPage(){
//        long current = 1;
//        long size = 20;
//        //查询数据库
//        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current,size));
//
//        return ResultUtils.success(questionBankPage);
//    }

//    @PostMapping("/list/page/vo")
//    @SentinelResource(value="listQuestionBankVOByPage",
//            blockHandler = "handleBlockException",
//            fallback = "handleFallback")
//    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
//                                                                       HttpServletRequest request){
//        long current = questionBankQueryRequest.getCurrent();
//        long size = questionBankQueryRequest.getPageSize();
//        //限制爬虫
//        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
//        //查询数据库
//        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current,size),
//                questionBankService.getQueryWrapper(questionBankQueryRequest));
//        //获取封装类
//
//        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
//
//    }
    /**
     * listQuestionBankVOByPage 降级操作：直接返回本地数据
     */
//    public BaseResponse<Page<QuestionBankVO>> handleFallback(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
//                                                             HttpServletRequest request, Throwable ex) {
//        log.error("触发降级操作，原因：{}", ex.getMessage());
//        // 可以返回本地数据或空数据
//        return ResultUtils.success(null);
//    }
//
//    /**
//     * listQuestionBankVOByPage 流控操作
//     * 限流：提示“系统压力过大，请耐心等待”
//     * 熔断：执行降级操作
//     */
//    public BaseResponse<Page<QuestionBankVO>> handleBlockException(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
//                                                                   HttpServletRequest request, BlockException ex) {
//        // 降级操作
//        if (ex instanceof DegradeException) {
//            return handleFallback(questionBankQueryRequest, request, ex);
//        }
//        // 限流操作
//        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
//    }


    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                         HttpServletRequest request){
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //补充查询条件，只查询当前登录用户的数据
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));

        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage));
    }

    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request){
        if(questionBankEditRequest == null || questionBankEditRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //todo 再次将实体类和DTO进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(request, questionBank);

        //数据校验
        questionBankService.validQuestionBank(questionBank, false);
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);

        //判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if(!oldQuestionBank.getUserId().equals(loginUser.getId())&& !userService.isAdmin()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }


}