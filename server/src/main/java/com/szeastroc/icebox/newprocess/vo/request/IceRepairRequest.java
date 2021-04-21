package com.szeastroc.icebox.newprocess.vo.request;

import com.szeastroc.icebox.newprocess.webservice.ObjectFactory;
import com.szeastroc.icebox.newprocess.webservice.WbSiteRequestVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotEmpty;

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
    @NotEmpty(message = "冰柜型号不能为空")
    @ApiModelProperty(value = "冰柜型号", required = true)
    private String modelName;
    private String saleOrderId;
    @NotEmpty(message = "预约日期不能为空")
    @ApiModelProperty(value = "预约日期", required = true)
    private String requireServiceDate;
    @NotEmpty(message = "姓名不能为空")
    @ApiModelProperty(value = "姓名", required = true)
    private String linkMan;
    @NotEmpty(message = "手机号不能为空")
    @ApiModelProperty(value = "手机号", required = true)
    private String linkMobile;
    @NotEmpty(message = "时间范围不能为空")
    @ApiModelProperty(value = "时间范围", required = true)
    private String bookingRange;
    private String serviceTypeId;
    @ApiModelProperty(value = "客户（门店/配送商）编号", required = true, example = "C0000001")
    private String customerNumber;
    @ApiModelProperty(value = "客户名称", required = true)
    private String customerName;
    @NotEmpty(message = "客户地址不能为空")
    @ApiModelProperty(value = "客户地址", required = true)
    private String customerAddress;
    @ApiModelProperty(value = "冰箱id", required = true)
    private Integer boxId;
    @ApiModelProperty(value = "资产编号", required = true)
    private String assetId;
    @ApiModelProperty(value = "客户类型", required = true, notes = "1经销商 2分销商 3邮差 4批发 5门店", example = "5")
    private Integer customerType;
    @ApiModelProperty(value = "维修备注", required = true)
    private String remark;
    @NotEmpty(message = "问题类型不能为空")
    @ApiModelProperty(value = "问题类型", required = true)
    private String description;
    @ApiModelProperty(value = "冰箱类型id", required = true)
    private Integer modelId;
    @ApiModelProperty(value = "省份", required = true)
    private String province;
    @ApiModelProperty(value = "省份编码", required = true)
    private String provinceCode;
    @ApiModelProperty(value = "城市", required = true)
    private String city;
    @ApiModelProperty(value = "城市编码", required = true)
    private String cityCode;
    @ApiModelProperty(value = "区县", required = true)
    private String area;
    @ApiModelProperty(value = "区县编码", required = true)
    private String areaCode;

    private String phoneAreaCode;


    public WbSiteRequestVO convertToWbSite() {
        if (StringUtils.isBlank(this.areaCode)) {
            this.areaCode = this.cityCode;
            if (StringUtils.isBlank(this.areaCode)) {
                this.areaCode = this.provinceCode;
            }
        }
        ObjectFactory objectFactory = new ObjectFactory();
        WbSiteRequestVO requestVO = objectFactory.createWbSiteRequestVO();
        requestVO.setPsnAccount(objectFactory.createWbSiteRequestVOPsnAccount(this.psnAccount));
        requestVO.setPsnPwd(objectFactory.createWbSiteRequestVOPsnPwd(this.psnPwd));
        requestVO.setOriginFlag(objectFactory.createWbSiteRequestVOOriginFlag(this.originFlag));
        if ("SC-200-1".equals(this.modelName)) {
            requestVO.setModelName(objectFactory.createWbSiteRequestVOModelName("SC-200"));
        } else {
            requestVO.setModelName(objectFactory.createWbSiteRequestVOModelName(this.modelName));
        }
        requestVO.setSaleOrderId(objectFactory.createWbSiteRequestVOSaleOrderId(this.saleOrderId));
        requestVO.setTelephone2(objectFactory.createWbSiteRequestVOTelephone2(this.linkMobile));
        requestVO.setAddress(objectFactory.createWbSiteRequestVOAddress(this.customerAddress));
        if (this.city.contains("儋州")) {
            requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId("469003000"));
        } else if (this.city.contains("中山")) {
            requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId("442000000"));
        } else if (this.city.contains("东莞")) {
            requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId("441904100"));
        } else {
            if (this.areaCode.length() > 6) {
                requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId(this.areaCode.substring(0, 6) + "000"));
            } else {
                requestVO.setRegoinId(objectFactory.createWbSiteRequestVORegoinId(this.areaCode + "000"));
            }
        }
        requestVO.setRequireServiceDate(objectFactory.createWbSiteRequestVORequireServiceDate(this.requireServiceDate));
        requestVO.setFaultDesc(objectFactory.createWbSiteRequestVOFaultDesc(this.description));
        requestVO.setCustomerName(objectFactory.createWbSiteRequestVOCustomerName(this.linkMan));
        requestVO.setBookingRange(objectFactory.createWbSiteRequestVOBookingRange(this.bookingRange));
        requestVO.setServiceTypeId(objectFactory.createWbSiteRequestVOServiceTypeId(this.serviceTypeId));
        requestVO.setAreaCode1(objectFactory.createWbSiteRequestVOAreaCode1(this.phoneAreaCode));
        return requestVO;
    }
}
