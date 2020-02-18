package com.szeastroc.icebox.oldprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.oldprocess.vo.MarketAreaVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Tulane
 * 2019/5/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_market_area")
public class MarketArea {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private Integer parentId;
    private String server;
    private String region;
    private String business;


    public MarketAreaVo convertVo() {
        MarketAreaVo marketAreaVo = new MarketAreaVo();
        marketAreaVo.setId(id);
        marketAreaVo.setName(name);
        marketAreaVo.setParentId(parentId);
        marketAreaVo.setServer(server);
        marketAreaVo.setRegion(region);
        marketAreaVo.setBusiness(business);
        return marketAreaVo;
    }
}
