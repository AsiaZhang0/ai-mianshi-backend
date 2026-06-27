package org.example.model.dto.questionBankQuestion;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestinoBankQuestionBatchAddRequest implements Serializable {
    /**
     * 题库id
     */
    private Long questionBankId;

    /**
     * 题目id列表
     */
    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}
