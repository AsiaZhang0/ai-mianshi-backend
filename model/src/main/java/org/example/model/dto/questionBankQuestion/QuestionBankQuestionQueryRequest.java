package org.example.model.dto.questionBankQuestion;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.common.request.PageRequest;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionBankQuestionQueryRequest extends  PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * id
     */
    private Long notId;

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

    private static final long serialVersionUID = 1L;

}