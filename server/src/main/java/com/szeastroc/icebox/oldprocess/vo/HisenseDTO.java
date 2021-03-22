package com.szeastroc.icebox.oldprocess.vo;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * HisenceDTO
 *
 * @author yuqi9
 * @since 2019/5/23
 */
public class HisenseDTO implements Serializable {


    private static final long serialVersionUID = -8777317011888606184L;
    /**
     * 推送事件类型 1.普通定时推送 2.温度变化  3.发生断点  4.GPS位置变化
     */
    private Integer type;


    /**
     * 资产id
     */
    private String controlId;

    /**
     * 事件发生时间
     */
    private Date occurrenceTime;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 开关门次数
     */
    private Integer openCloseCount;

    /**
     * 经度
     */
    private String lng;

    /**
     * 纬度
     */
    private String lat;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 签名信息
     */
    private String sign;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public Date getOccurrenceTime() {
        return occurrenceTime;
    }

    public void setOccurrenceTime(Date occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getOpenCloseCount() {
        return openCloseCount;
    }

    public void setOpenCloseCount(Integer openCloseCount) {
        this.openCloseCount = openCloseCount;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public boolean validate(){
        if(null == this.type || StringUtils.isBlank(controlId) || null == occurrenceTime || null == temperature || null == openCloseCount ){
            return true;
        }
        if(this.temperature > 100 || this.temperature < -100){
            return true;
        }
        return false;
    }
}
