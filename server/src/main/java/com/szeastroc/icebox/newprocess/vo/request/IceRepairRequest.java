package com.szeastroc.icebox.newprocess.vo.request;

import com.szeastroc.icebox.newprocess.webservice.ObjectFactory;
import com.szeastroc.icebox.newprocess.webservice.WbSiteRequestVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: RepairIceDTO
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/12 14:40
 **/
@Data
@ApiModel
public class IceRepairRequest {

    private String psnAccount;
    private String psnPwd;
    private String originFlag;
    @ApiModelProperty(value = "冰柜型号",required = true)
    private String modelName;
    private String saleOrderId;
    @ApiModelProperty(value = "预约日期",required = true)
    private String requireServiceDate;
    @ApiModelProperty(value = "姓名",required = true)
    private String linkMan;
    @ApiModelProperty(value = "手机号",required = true)
    private String linkMobile;
    @ApiModelProperty(value = "时间范围",required = true)
    private String bookingRange;
    private String serviceTypeId;
    @ApiModelProperty(value = "客户（门店/配送商）编号",required = true,example = "C0000001")
    private String customerNumber;
    @ApiModelProperty(value = "客户名称",required = true)
    private String customerName;
    @ApiModelProperty(value = "客户地址",required = true)
    private String customerAddress;
    @ApiModelProperty(value = "冰箱id",required = true)
    private Integer boxId;
    @ApiModelProperty(value = "资产编号",required = true)
    private String assetId;
    @ApiModelProperty(value = "客户类型",required = true,notes = "1经销商 2分销商 3邮差 4批发 5门店",example = "5")
    private Integer customerType;
    @ApiModelProperty(value = "维修备注",required = true)
    private String remark;
    @ApiModelProperty(value = "问题类型",required = true)
    private String description;
    @ApiModelProperty(value = "冰箱类型id",required = true)
    private Integer modelId;
    @ApiModelProperty(value = "省份",required = true)
    private String province;
    @ApiModelProperty(value = "省份编码",required = true)
    private String provinceCode;
    @ApiModelProperty(value = "城市",required = true)
    private String city;
    @ApiModelProperty(value = "城市编码",required = true)
    private String cityCode;
    @ApiModelProperty(value = "区县",required = true)
    private String area;
    @ApiModelProperty(value = "区县编码",required = true)
    private String areaCode;



    public WbSiteRequestVO convertToWbSite() {
        ObjectFactory objectFactory = new ObjectFactory();
        WbSiteRequestVO requestVO = objectFactory.createWbSiteRequestVO();
        requestVO.setPsnAccount(objectFactory.createWbSiteRequestVOPsnAccount(this.psnAccount));
        requestVO.setPsnPwd(objectFactory.createWbSiteRequestVOPsnPwd(this.psnPwd));
        requestVO.setOriginFlag(objectFactory.createWbSiteRequestVOOriginFlag(this.originFlag));
        requestVO.setModelName(objectFactory.createWbSiteRequestVOModelName(this.modelName));
        requestVO.setSaleOrderId(objectFactory.createWbSiteRequestVOSaleOrderId(this.saleOrderId));
        requestVO.setTelephone2(objectFactory.createWbSiteRequestVOTelephone2(this.linkMobile));
        requestVO.setAddress(objectFactory.createWbSiteRequestVOAddress(this.customerAddress));
        requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId(this.areaCode));
        requestVO.setRequireServiceDate(objectFactory.createWbSiteRequestVORequireServiceDate(this.requireServiceDate));
        requestVO.setFaultDesc(objectFactory.createWbSiteRequestVOFaultDesc(this.description));
        requestVO.setCustomerName(objectFactory.createWbSiteRequestVOCustomerName(this.linkMan));
        requestVO.setBookingRange(objectFactory.createWbSiteRequestVOBookingRange(this.bookingRange));
        requestVO.setServiceTypeId(objectFactory.createWbSiteRequestVOServiceTypeId(this.serviceTypeId));
//        requestVO.setAreaCode1(objectFactory.createWbSiteRequestVOAreaCode1(thi));
        return requestVO;
    }
}
