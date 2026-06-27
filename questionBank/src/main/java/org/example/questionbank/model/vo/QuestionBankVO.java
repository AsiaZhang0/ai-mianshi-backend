package org.example.questionbank.model.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.example.model.vo.QuestionVO;
import org.example.model.vo.UserVO;
import org.example.model.entity.QuestionBank;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class QuestionBankVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */

    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 图片
     */
    private String picture;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 题库里的题目列表（分页）
     */
    Page<QuestionVO> questionPage;

    public static QuestionBank voToObj(QuestionBankVO questionBankVO){
        if(questionBankVO == null){
            return null;
        }
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankVO,questionBank);
        return questionBank;
    }

    public static QuestionBankVO objToVo(QuestionBank questionBank){
        if(questionBank == null){
            return null;
        }
        QuestionBankVO questionBankVO = new QuestionBankVO();
        BeanUtils.copyProperties(questionBank,questionBankVO);
        return questionBankVO;
    }


}
