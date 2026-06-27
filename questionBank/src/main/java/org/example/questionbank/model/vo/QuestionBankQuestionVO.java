package org.example.questionbank.model.vo;

import lombok.Data;
import org.example.model.vo.UserVO;
import org.example.model.entity.QuestionBankQuestion;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class QuestionBankQuestionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 题库id
     */
    private Long questionBankId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     */

    public static QuestionBankQuestion voToObj(QuestionBankQuestionVO questionBankQuestionVO){
        if(questionBankQuestionVO == null){
            return null;
        }
        QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
        BeanUtils.copyProperties(questionBankQuestionVO, questionBankQuestion);

        return questionBankQuestion;
    }

    public static QuestionBankQuestionVO objToVo(QuestionBankQuestion questionBankQuestion){
        if(questionBankQuestion == null){
            return null;
        }
        QuestionBankQuestionVO questionBankQuestionVO = new QuestionBankQuestionVO();
        BeanUtils.copyProperties(questionBankQuestion, questionBankQuestionVO);
        return questionBankQuestionVO;
    }
}

