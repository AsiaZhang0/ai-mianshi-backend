package org.example.questions.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
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
import org.example.questions.kafka.QuestionEventProducer;
import org.example.questions.model.dto.QuestionAddRequest;
import org.example.questions.model.dto.QuestionEditRequest;
import org.example.questions.model.dto.QuestionUpdateRequest;

import org.example.questions.service.QuestionService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class QuestionController {
    @Resource
    private QuestionService questionService;

    @DubboReference(check = false)
    private InnerUserService userService;

    @Resource
    private QuestionEventProducer questionEventProducer;

//    @Resource
//    private CounterManager counterManager;

    /**
     * 检测爬虫
     * @param loginUserId
     */
//    private void crawlerDetect(long loginUserId){
//        //调用多少次时告警
//        final int WARN_COUNT = 10;
//
//        //超过多少次封号
//        final int BAN_COUNT = 20;
//
//        //拼接访问key
//        String key = String.format("user:access:%s", loginUserId);
//
//        //一分钟内访问次数，180秒过期
//        long count = counterManager.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 180);
//
//        //是否封号
//        if(count > BAN_COUNT){
//            //踢下线
//            StpUtil.kickout(loginUserId);
//            //封号
//            User updateUser = new User();
//            updateUser.setId(loginUserId);
//            updateUser.setUserRole("ban");
//            userService.updateById(updateUser);
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问太频繁，已被封号");
//        }
//
//        //是否告警
//        if(count == WARN_COUNT){
//            // 可以改为向管理员发送邮件通知
//            throw new BusinessException(110, "访问太频繁，请稍后再试");
//        }
//    }

    /**
     * 创建题目
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request){
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        //todo 在此处将实体类和DTO进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if(tags != null){
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        //数据校验
        questionService.validQuestion(question, true);
        //todo 填充默认值
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);
        question.setUserId(loginUser.getId());

        //写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //返回新写入的数据id
        long newQuestionId = question.getId();

        // 发送 Kafka 消息通知 agent 模块生成嵌入向量
        questionEventProducer.sendQuestionCreated(question);

        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long loginUserId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(loginUserId);
        long id = deleteRequest.getId();

        //判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        if(!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 发送 Kafka 消息通知 agent 模块删除向量
        questionEventProducer.sendQuestionDeleted(id);

        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest){
        if(questionUpdateRequest == null || questionUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和DTO进行转化
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);

        //数据校验
        questionService.validQuestion(question, false);
        //判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        //操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 发送 Kafka 消息通知 agent 模块更新向量
        questionEventProducer.sendQuestionUpdated(question);

        return ResultUtils.success(true);
    }

    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(Long id, HttpServletRequest request){

        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        log.info("getQuestionVOById id={}", id);
        long loginUserId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(loginUserId);
//        crawlerDetect(user.getId());
        //查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);

        //获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question));
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类 - 限流版）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
//    @PostMapping("/list/page/vo/sentinel")
//    public BaseResponse<Page<QuestionVO>> listQuestionVOByPageSentinel(@RequestBody QuestionQueryRequest questionQueryRequest,
//                                                                       HttpServletRequest request) {
//        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
//        long size = questionQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 基于 IP 限流
//        String remoteAddr = request.getRemoteAddr();
//        Entry entry = null;
//        try {
//            entry = SphU.entry(SentinelConstant.listQuestionVOByPage, EntryType.IN, 1, remoteAddr);
//            // 被保护的业务逻辑
//            // 查询数据库
//            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
//            // 获取封装类
//            return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
//        } catch (Throwable ex) {
//            // 业务异常
//            if (!BlockException.isBlockException(ex)) {
//                Tracer.trace(ex);
//                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
//            }
//            // 降级操作
//            if (ex instanceof DegradeException) {
//                return handleFallback(questionQueryRequest, request, ex);
//            }
//            // 限流操作
//            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "访问过于频繁，请稍后再试");
//        } finally {
//            if (entry != null) {
//                entry.exit(1, remoteAddr);
//            }
//        }
//    }

    /**
     * listQuestionVOByPageSentinel 降级操作：直接返回本地数据（此处为了方便演示，写在同一个类中）
     */
//    public BaseResponse<Page<QuestionVO>> handleFallback(@RequestBody QuestionQueryRequest questionQueryRequest,
//                                                         HttpServletRequest request, Throwable ex) {
//        // 可以返回本地数据或空数据
//        return ResultUtils.success(null);
//    }



    /**
     * 分页获取题目列表（封装类）
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest){
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long size = questionQueryRequest.getPageSize();
        long current = questionQueryRequest.getCurrent();
        //限制爬虫
        ThrowUtils.throwIf((current != 1 || size > 50) && !StpUtil.isLogin(), ErrorCode.NO_AUTH_ERROR);
        //查询数据库
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        //获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage));
    }


    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request){
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);

        //补充查询条件，只查询当前登录用户的数据
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        //获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request){
        if(questionEditRequest == null || questionEditRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和DTO进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if(tags != null){
            question.setTags(JSONUtil.toJsonStr(tags));
        }

        //数据校验
        questionService.validQuestion(question, false);
        long loginUserId = StpUtil.getLoginIdAsLong();
        User loginUser = userService.getById(loginUserId);

        //判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        //仅本人或管理员可编辑
        if(!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }


    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage));
    }


}
