package com.szeastroc.icebox.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Created by Tulane
 * 2019/5/28
 */
@Getter
@Setter
public class MarketAreaVo {

    private Integer id;
    private String name;
    private Integer parentId;
    private String server;
    private String region;
    private String business;

}
