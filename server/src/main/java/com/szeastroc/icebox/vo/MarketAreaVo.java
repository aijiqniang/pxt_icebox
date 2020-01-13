package com.szeastroc.icebox.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Tulane
 * 2019/5/28
 */
public class MarketAreaVo {

    private Integer id;
    private String name;
    private Integer parentId;
    private String server;
    private String region;
    private String business;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setBusiness(String business) {
        this.business = business;
    }
}
