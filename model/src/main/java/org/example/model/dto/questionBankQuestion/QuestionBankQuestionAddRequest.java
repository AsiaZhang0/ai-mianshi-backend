package org.example.model.dto.questionBankQuestion;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionBankQuestionAddRequest implements Serializable {


    /**
     *  题库id
     */
    private Long questionBankId;

    /**
     * 题库id
     */
    private Long questionId;

    private static final long serialVersionUID = 1L;

}
