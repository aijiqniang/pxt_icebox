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
public class OrderPayResponse {

    private String appId;
    private String timeStamp;
    private String nonceStr;
    private String packageStr;
    private String signType;
    private String paySign;
    private String orderNum;
}
