package com.szeastroc.icebox.oldprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * Created by Tulane
 * 2019/5/22
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfoRequest {

    private String clientName;
    private String clientNumber;
    private String clientPlace;
    private String clientLevel;
    private String contactName;
    private String contactMobile;
    private String marketAreaId;
    private String iceChestId;
    private String ip;
    private String openid;
    private String qrcode;
    /**
     * 数据来源：1-otoc,2-dms
     */
    private Integer orderSource;
    /**
     * 旧冰柜通知类型：1-已投放有冰柜id,2-未投放无冰柜id
     */
    private Integer type;
    /**
     *审核备注
     */
    private String examineRemark;

    public boolean validate(){
//        if(StringUtils.isBlank(clientName) || StringUtils.isBlank(clientNumber) || StringUtils.isBlank(marketAreaId)
//                || StringUtils.isBlank(iceChestId) || StringUtils.isBlank(ip) || StringUtils.isBlank(openid))
        //modify by hbl 20200427 商户小程序没有营销区域id，需要在自己查
        if(StringUtils.isBlank(clientName) || StringUtils.isBlank(clientNumber) || StringUtils.isBlank(iceChestId)
                || StringUtils.isBlank(ip) || StringUtils.isBlank(openid)){
            return false;
        }
        return true;
    }
}
