package com.szeastroc.icebox.newprocess.webservice;

import cn.hutool.core.util.RandomUtil;
import org.joda.time.DateTime;

/**
 * @ClassName: Test
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/12 9:32
 **/
public class Test {

    public static void main(String[] args) throws Exception {
        ObjectFactory objectFactory = new ObjectFactory();
        WbSiteRequestVO requestVO = objectFactory.createWbSiteRequestVO();
        requestVO.setPsnAccount(objectFactory.createWbSiteRequestVOPsnAccount("website"));
        requestVO.setPsnPwd(objectFactory.createWbSiteRequestVOPsnPwd("Aa666666"));
        requestVO.setOriginFlag(objectFactory.createWbSiteRequestVOOriginFlag("DP"));
        requestVO.setModelName(objectFactory.createWbSiteRequestVOModelName("SC-518WYSL/HP"));
        String orderId = "REP"+new DateTime().toString("yyyyMMddHHmmss")+ RandomUtil.randomNumbers(4);
        requestVO.setSaleOrderId(objectFactory.createWbSiteRequestVOSaleOrderId(orderId));
        requestVO.setTelephone2(objectFactory.createWbSiteRequestVOTelephone2("18672128394"));
        requestVO.setAreaCode1(objectFactory.createWbSiteRequestVOAreaCode1("0755"));
        requestVO.setAddress(objectFactory.createWbSiteRequestVOAddress("明亮科技园"));
        requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId("440300000"));
        requestVO.setRequireServiceDate(objectFactory.createWbSiteRequestVORequireServiceDate("2021-01-15"));
        requestVO.setFaultDesc(objectFactory.createWbSiteRequestVOFaultDesc("冰柜坏了"));
        requestVO.setCustomerName(objectFactory.createWbSiteRequestVOCustomerName("小陈"));
        requestVO.setBookingRange(objectFactory.createWbSiteRequestVOBookingRange("上午"));
        requestVO.setServiceTypeId(objectFactory.createWbSiteRequestVOServiceTypeId("WX"));
        WebSite webSite = new WebSite();
        WebSitePortType httpEndpoint = webSite.getWebSiteHttpSoap12Endpoint();
        String str = JaxbUtil.convertToXml(requestVO);
        System.out.println(str);
        WbSiteResponseVO responseVO = httpEndpoint.getWBSite(requestVO);
        String response = JaxbUtil.convertToXml(responseVO);
        System.out.println(response);

    }
}
