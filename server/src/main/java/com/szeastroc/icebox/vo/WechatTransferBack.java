package com.szeastroc.icebox.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Tulane
 * 2019/5/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatTransferBack {

    private String returnCode;
    private String returnMsg;

    private String resultCode;
    private String mchAppid;
    private String mchid;
    private String nonceStr;
    private String errCode;
    private String errCodeDes;

    private String partnerTradeNo;
    private String paymentNo;
    private String paymentTime;
}
