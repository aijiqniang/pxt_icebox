package com.szeastroc.icebox.oldprocess.vo;

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
public class OrderPayBack {

    private String returnCode;
    private String returnMsg;
    private String openid;
    private String totalFee;
    private String outTradeNo;
    private String transactionId;
    private String timeEnd;
    private String resultCode;
    private String tradeState;
    private String tradeStateDesc;
}
