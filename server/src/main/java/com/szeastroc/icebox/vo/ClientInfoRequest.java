package com.szeastroc.icebox.vo;

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
    private String marketAreas;

    public boolean validate(){
        if(StringUtils.isBlank(clientName) || StringUtils.isBlank(clientNumber) || StringUtils.isBlank(marketAreaId)
                || StringUtils.isBlank(iceChestId) || StringUtils.isBlank(ip) || StringUtils.isBlank(openid) || StringUtils.isBlank(marketAreas)){
            return false;
        }
        return true;
    }
}
