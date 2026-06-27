package org.example.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("log")
public class Log implements Serializable {
    @TableId(type= IdType.ASSIGN_UUID)
    private String id;

    private Long userId;

    private String content;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
