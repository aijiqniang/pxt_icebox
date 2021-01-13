package com.szeastroc.icebox.newprocess.vo.request;

import com.szeastroc.icebox.newprocess.webservice.ObjectFactory;
import com.szeastroc.icebox.newprocess.webservice.WbSiteRequestVO;
import lombok.Data;

/**
 * @ClassName: RepairIceDTO
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/12 14:40
 **/
@Data
public class IceRepairRequest {

    private String psnAccount;
    private String psnPwd;
    private String originFlag;
    private String modelName;
    private String saleOrderId;
    private String requireServiceDate;
    private String linkMan;
    private String linkMobile;
    private String bookingRange;
    private String serviceTypeId;
    private String prodSerialNo;
    private String customerNumber;
    private String customerName;
    private String customerAddress;
    private Integer boxId;
    private String assetId;
    private Integer customerType;
    private String remark;
    private String description;
    private Integer modelId;
    private String province;
    private String provinceCode;
    private String city;
    private String cityCode;
    private String area;
    private String areaCode;



    public WbSiteRequestVO convertToWbSite() {
        ObjectFactory objectFactory = new ObjectFactory();
        WbSiteRequestVO requestVO = objectFactory.createWbSiteRequestVO();
        requestVO.setPsnAccount(objectFactory.createWbSiteRequestVOPsnAccount(this.psnAccount));
        requestVO.setPsnPwd(objectFactory.createWbSiteRequestVOPsnPwd(this.psnPwd));
        requestVO.setOriginFlag(objectFactory.createWbSiteRequestVOOriginFlag(this.originFlag));
        requestVO.setModelName(objectFactory.createWbSiteRequestVOModelName(this.modelName));
        requestVO.setSaleOrderId(objectFactory.createWbSiteRequestVOSaleOrderId(this.saleOrderId));
        requestVO.setTelephone1(objectFactory.createWbSiteRequestVOTelephone1(this.linkMobile));
        requestVO.setAddress(objectFactory.createWbSiteRequestVOAddress(this.customerAddress));
        requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId(this.areaCode));
        requestVO.setRequireServiceDate(objectFactory.createWbSiteRequestVORequireServiceDate(this.requireServiceDate));
        requestVO.setFaultDesc(objectFactory.createWbSiteRequestVOFaultDesc(this.description));
        requestVO.setCustomerName(objectFactory.createWbSiteRequestVOCustomerName(this.linkMan));
        requestVO.setBookingRange(objectFactory.createWbSiteRequestVOBookingRange(this.bookingRange));
        requestVO.setServiceTypeId(objectFactory.createWbSiteRequestVOServiceTypeId(this.serviceTypeId));
        requestVO.setProdSerialNo(objectFactory.createWbSiteRequestVOProdSerialNo(this.prodSerialNo));
        return requestVO;
    }
}
