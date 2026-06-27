package org.example.questions.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.example.client.service.InnerQuestionBankQuestionService;
import org.example.client.service.InnerUserService;
import org.example.common.constant.CommonConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.SqlUtils;
import org.example.common.exception.ThrowUtils;

import org.example.model.dto.question.QuestionQueryRequest;
import org.example.model.entity.Question;
import org.example.model.entity.QuestionBankQuestion;
import org.example.model.entity.User;
import org.example.model.vo.QuestionVO;
import org.example.model.vo.UserVO;
import org.example.questions.mapper.QuestionMapper;

import org.example.questions.model.dto.QuestionEsRequest;

import org.example.questions.service.QuestionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper,Question> implements QuestionService {

    @Resource
    private InnerUserService userService;

    @DubboReference(check = false)
    private InnerQuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ElasticsearchTemplate elasticsearchRestTemplate;

    /**
     * 校验数据
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);

        //todo 从对象中取值
        String title = question.getTitle();
        String content = question.getContent();

        //创建数据时，参数不能为空
        if(add){
            //todo 不从校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }

        //修改数据时，有参数则校验
        //todo 补充校验规则
        if(StringUtils.isNotBlank(title)){
            ThrowUtils.throwIf(title.length()>80, ErrorCode.PARAMS_ERROR,"标题过长");

        }
        if(StringUtils.isNotBlank(content)){
            ThrowUtils.throwIf(content.length()> 10240, ErrorCode.PARAMS_ERROR,"内容过长");
        }
    }

    /**
     * 获取查询条件
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if(questionQueryRequest == null){
            return queryWrapper;
        }

        //todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();

        //todo 补充需要的查询条件
        //从多个字段中搜索
        if(StringUtils.isNotBlank(searchText)){
            //需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title",searchText).or().like("content",searchText));
        }
        //模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title),"title",title);
        queryWrapper.like(StringUtils.isNotBlank(content),"content",content);
        queryWrapper.like(StringUtils.isNotBlank(answer),"answer",answer);
        //JSON 数组查询
        if(CollUtil.isNotEmpty(tagList)){
            for(String tag : tagList){
                queryWrapper.like("tags","\""+tag+"\"");
            }
        }
        //精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId),"id",notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);

        //排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question) {
        //对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        //todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        //region 可选
        //1.关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if(userId != null && userId > 0){
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);

        return questionVO;
    }

    /**
     * 分页获取题目封装
     * @param questionPage
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(),questionPage.getSize(),questionPage.getTotal());
        if(CollUtil.isEmpty(questionList)){
            return questionVOPage;
        }
        //对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        //todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        //region 可选
        //1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long,List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        //题目表的查询条件
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);
        //格局题库查询题目列表接口
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if(questionBankId != null){
            //查询题库内的题目id
            List<QuestionBankQuestion> questionList = questionBankQuestionService.listByQuestionBankId(questionBankId);
            if(CollUtil.isNotEmpty(questionList)){
                //取出题目id 集合
                Set<Long> questionIdSet = questionList.stream()
                        .map(QuestionBankQuestion::getQuestionId)
                        .collect(Collectors.toSet());
                //复用原有题目表的查询条件
                queryWrapper.in("id",questionIdSet);
            }else{
                return new Page<>(current,size,0);
            }
        }
        //查询数据库
        Page<Question> questionPage = this.page(new Page<>(current,size),queryWrapper);
        return questionPage;
    }

    /**
     * 从 ES 查询题目
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        // 获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String searchText = questionQueryRequest.getSearchText();
        List<String> tags = questionQueryRequest.getTags();
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        Long userId = questionQueryRequest.getUserId();
        // 注意，ES 的起始页为 0
        int current = questionQueryRequest.getCurrent() - 1;
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 构造 BoolQuery
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        // 过滤：isDelete = 0
        boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("isDelete").value(0))));
        if (id != null) {
            boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("id").value(id))));
        }
        if (notId != null) {
            boolQueryBuilder.mustNot(Query.of(q -> q.term(t -> t.field("id").value(notId))));
        }
        if (userId != null) {
            boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("userId").value(userId))));
        }
        if (questionBankId != null) {
            boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("questionBankId").value(questionBankId))));
        }
        // 必须包含所有标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("tags").value(tag))));
            }
        }
        // 按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(Query.of(q -> q.match(m -> m.field("title").query(searchText))));
            boolQueryBuilder.should(Query.of(q -> q.match(m -> m.field("content").query(searchText))));
            boolQueryBuilder.should(Query.of(q -> q.match(m -> m.field("answer").query(searchText))));
            boolQueryBuilder.minimumShouldMatch("1");
        }
        // 构造 NativeQuery
        NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(PageRequest.of(current, pageSize));
        // 排序
        if (StringUtils.isNotBlank(sortField)) {
            SortOrder esSortOrder = CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
            nativeQueryBuilder.withSort(
                    SortOptions.of(s -> s.field(f -> f.field(sortField).order(esSortOrder)))
            );
        }
        // 执行查询
        SearchHits<QuestionEsRequest> searchHits = elasticsearchRestTemplate.search(
                nativeQueryBuilder.build(), QuestionEsRequest.class);
        // 复用 MySQL / MyBatis Plus 的分页对象，封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Question> resourceList = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            for (SearchHit<QuestionEsRequest> hit : searchHits.getSearchHits()) {
                resourceList.add(QuestionEsRequest.dtoToObj(hit.getContent()));
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    /**
     * 批量删除题目
     *
     * @param questionIdList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "要删除的题目列表不能为空");
        for (Long questionId : questionIdList) {
            boolean result = this.removeById(questionId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目失败");
            // 移除题目题库关系
            result = questionBankQuestionService.removeByQuestionId(questionId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目题库关联失败");
        }
    }

}
