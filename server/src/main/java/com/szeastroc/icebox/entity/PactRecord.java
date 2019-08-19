package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_pact_record")
public class PactRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer clientId;
    private Integer chestId;
    private Integer putId;
    private Date putTime;
    private Date putExpireTime;
    private Date createTime;
    private Date updateTime;

    public PactRecord(Integer clientId, Integer chestId) {
        this.clientId = clientId;
        this.chestId = chestId;
        this.createTime = new Date();
        this.updateTime = this.createTime;
    }
}
