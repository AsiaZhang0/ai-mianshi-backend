package org.example.model.dto.questionBankQuestion;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionBankQuestionUpdateRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;

}
