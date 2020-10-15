package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_old_ice_box_sign_notice")
public class OldIceBoxSignNotice {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 冰柜id
     */
    private Integer iceBoxId;
    /**
     * 冰柜资产编号
     */
    private String assetId;
    /**
     * 投放门店或配送商编号
     */
    private String putStoreNumber;
    /**
     * 签收状态：0-未签收，1-已签收
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}
