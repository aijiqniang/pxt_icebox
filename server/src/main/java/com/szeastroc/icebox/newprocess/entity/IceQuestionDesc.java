package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * (IceQuestionDesc)表实体类
 *
 * @author chenchao
 * @since 2021-01-18 09:42:09
 */
@SuppressWarnings("serial")
@Data
@TableName("t_ice_question_desc")
public class IceQuestionDesc extends Model<IceQuestionDesc> {


    private Integer id;


    /**
     * 冰柜问题描述
     */

    private String description;


    private Date createTime;


    private Date updateTime;


    /**
     * 获取主键值
     *
     * @return 主键值
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}