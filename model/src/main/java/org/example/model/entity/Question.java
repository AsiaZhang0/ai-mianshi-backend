package org.example.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value="question")
@Data
public class Question implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /*
     * 标题
     */
    private String title;

    /*
     * 难度
     */
    private Integer difficultyLevel;
    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表(json 数组)
     */
    private String tags;

    /**
     * 推荐答案
     */
    private String answer;

    /*
     * 创建用户id
     */
    private Long userId;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID=1L;
}