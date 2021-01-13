
package com.szeastroc.icebox.newprocess.webservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>WbSiteRequestVO complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
 * <pre>
 * &lt;complexType name="WbSiteRequestVO">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="active_flag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="area_code1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute10" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute11" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute12" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute13" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute14" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute15" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute16" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute17" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute18" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute19" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute20" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute4" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute5" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute6" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute7" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute8" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attribute9" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="barter_flag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="booking_range" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="buy_date" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="created_by" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="created_date" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="customer_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="customer_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="error_info" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fault_desc" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="if_flush" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="if_pull_back" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="if_service" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="image_url" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="import_flag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="jd_wb_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="last_upd_by" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="last_upd_date" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="logo_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="logo_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ls_order" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="market_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="market_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mid_product_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mid_product_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="model_code" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="model_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="model_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="modification_num" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="open_flag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="origin_flag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="price" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="prod_serial_no" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="prod_type_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="project_no" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="psn_account" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="psn_pwd" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="regoin_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="remark" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="require_desc" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="require_service_date" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="road_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="row_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sale_order_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sale_order_no" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sale_order_no_son" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="server_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="service_mode_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="service_type_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="telephone1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="telephone2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="telephone3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="total_num" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="user_ip" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="user_ip_date" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="video_url" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="we_chat_nc" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="wx_openid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="wz_customer_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="wz_row_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="zx_content" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="zx_mode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WbSiteRequestVO", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", propOrder = {
    "activeFlag",
    "address",
    "areaCode1",
    "attribute1",
    "attribute10",
    "attribute11",
    "attribute12",
    "attribute13",
    "attribute14",
    "attribute15",
    "attribute16",
    "attribute17",
    "attribute18",
    "attribute19",
    "attribute2",
    "attribute20",
    "attribute3",
    "attribute4",
    "attribute5",
    "attribute6",
    "attribute7",
    "attribute8",
    "attribute9",
    "barterFlag",
    "bookingRange",
    "buyDate",
    "createdBy",
    "createdDate",
    "customerId",
    "customerName",
    "email",
    "errorInfo",
    "faultDesc",
    "ifFlush",
    "ifPullBack",
    "ifService",
    "imageUrl",
    "importFlag",
    "jdWbId",
    "lastUpdBy",
    "lastUpdDate",
    "logoId",
    "logoName",
    "lsOrder",
    "marketId",
    "marketName",
    "midProductId",
    "midProductName",
    "modelCode",
    "modelId",
    "modelName",
    "modificationNum",
    "openFlag",
    "originFlag",
    "price",
    "prodSerialNo",
    "prodTypeId",
    "projectNo",
    "psnAccount",
    "psnPwd",
    "regoinId",
    "remark",
    "requireDesc",
    "requireServiceDate",
    "roadId",
    "rowId",
    "saleOrderId",
    "saleOrderNo",
    "saleOrderNoSon",
    "serverId",
    "serviceModeId",
    "serviceTypeId",
    "telephone1",
    "telephone2",
    "telephone3",
    "totalNum",
    "userIp",
    "userIpDate",
    "videoUrl",
    "weChatNc",
    "wxOpenid",
    "wzCustomerId",
    "wzRowId",
    "zxContent",
    "zxMode"
})
@XmlRootElement
public class WbSiteRequestVO {

    @XmlElementRef(name = "active_flag", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> activeFlag;
    @XmlElementRef(name = "address", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> address;
    @XmlElementRef(name = "area_code1", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> areaCode1;
    @XmlElementRef(name = "attribute1", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute1;
    @XmlElementRef(name = "attribute10", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute10;
    @XmlElementRef(name = "attribute11", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute11;
    @XmlElementRef(name = "attribute12", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute12;
    @XmlElementRef(name = "attribute13", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute13;
    @XmlElementRef(name = "attribute14", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute14;
    @XmlElementRef(name = "attribute15", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute15;
    @XmlElementRef(name = "attribute16", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute16;
    @XmlElementRef(name = "attribute17", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute17;
    @XmlElementRef(name = "attribute18", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute18;
    @XmlElementRef(name = "attribute19", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute19;
    @XmlElementRef(name = "attribute2", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute2;
    @XmlElementRef(name = "attribute20", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute20;
    @XmlElementRef(name = "attribute3", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute3;
    @XmlElementRef(name = "attribute4", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute4;
    @XmlElementRef(name = "attribute5", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute5;
    @XmlElementRef(name = "attribute6", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute6;
    @XmlElementRef(name = "attribute7", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute7;
    @XmlElementRef(name = "attribute8", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute8;
    @XmlElementRef(name = "attribute9", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> attribute9;
    @XmlElementRef(name = "barter_flag", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> barterFlag;
    @XmlElementRef(name = "booking_range", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> bookingRange;
    @XmlElementRef(name = "buy_date", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> buyDate;
    @XmlElementRef(name = "created_by", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> createdBy;
    @XmlElementRef(name = "created_date", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> createdDate;
    @XmlElementRef(name = "customer_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> customerId;
    @XmlElementRef(name = "customer_name", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> customerName;
    @XmlElementRef(name = "email", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> email;
    @XmlElementRef(name = "error_info", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> errorInfo;
    @XmlElementRef(name = "fault_desc", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> faultDesc;
    @XmlElementRef(name = "if_flush", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ifFlush;
    @XmlElementRef(name = "if_pull_back", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ifPullBack;
    @XmlElementRef(name = "if_service", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ifService;
    @XmlElementRef(name = "image_url", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> imageUrl;
    @XmlElementRef(name = "import_flag", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> importFlag;
    @XmlElementRef(name = "jd_wb_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> jdWbId;
    @XmlElementRef(name = "last_upd_by", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> lastUpdBy;
    @XmlElementRef(name = "last_upd_date", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> lastUpdDate;
    @XmlElementRef(name = "logo_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> logoId;
    @XmlElementRef(name = "logo_name", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> logoName;
    @XmlElementRef(name = "ls_order", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> lsOrder;
    @XmlElementRef(name = "market_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> marketId;
    @XmlElementRef(name = "market_name", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> marketName;
    @XmlElementRef(name = "mid_product_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> midProductId;
    @XmlElementRef(name = "mid_product_name", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> midProductName;
    @XmlElementRef(name = "model_code", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> modelCode;
    @XmlElementRef(name = "model_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> modelId;
    @XmlElementRef(name = "model_name", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> modelName;
    @XmlElementRef(name = "modification_num", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> modificationNum;
    @XmlElementRef(name = "open_flag", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> openFlag;
    @XmlElementRef(name = "origin_flag", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> originFlag;
    @XmlElementRef(name = "price", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> price;
    @XmlElementRef(name = "prod_serial_no", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> prodSerialNo;
    @XmlElementRef(name = "prod_type_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> prodTypeId;
    @XmlElementRef(name = "project_no", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> projectNo;
    @XmlElementRef(name = "psn_account", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> psnAccount;
    @XmlElementRef(name = "psn_pwd", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> psnPwd;
    @XmlElementRef(name = "regoin_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> regoinId;
    @XmlElementRef(name = "remark", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> remark;
    @XmlElementRef(name = "require_desc", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> requireDesc;
    @XmlElementRef(name = "require_service_date", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> requireServiceDate;
    @XmlElementRef(name = "road_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> roadId;
    @XmlElementRef(name = "row_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> rowId;
    @XmlElementRef(name = "sale_order_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> saleOrderId;
    @XmlElementRef(name = "sale_order_no", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> saleOrderNo;
    @XmlElementRef(name = "sale_order_no_son", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> saleOrderNoSon;
    @XmlElementRef(name = "server_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> serverId;
    @XmlElementRef(name = "service_mode_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> serviceModeId;
    @XmlElementRef(name = "service_type_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> serviceTypeId;
    @XmlElementRef(name = "telephone1", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> telephone1;
    @XmlElementRef(name = "telephone2", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> telephone2;
    @XmlElementRef(name = "telephone3", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> telephone3;
    @XmlElement(name = "total_num")
    protected Integer totalNum;
    @XmlElementRef(name = "user_ip", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> userIp;
    @XmlElementRef(name = "user_ip_date", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> userIpDate;
    @XmlElementRef(name = "video_url", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> videoUrl;
    @XmlElementRef(name = "we_chat_nc", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> weChatNc;
    @XmlElementRef(name = "wx_openid", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> wxOpenid;
    @XmlElementRef(name = "wz_customer_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> wzCustomerId;
    @XmlElementRef(name = "wz_row_id", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> wzRowId;
    @XmlElementRef(name = "zx_content", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> zxContent;
    @XmlElementRef(name = "zx_mode", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> zxMode;

    /**
     * 获取activeFlag属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getActiveFlag() {
        return activeFlag;
    }

    /**
     * 设置activeFlag属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setActiveFlag(JAXBElement<String> value) {
        this.activeFlag = value;
    }

    /**
     * 获取address属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAddress() {
        return address;
    }

    /**
     * 设置address属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAddress(JAXBElement<String> value) {
        this.address = value;
    }

    /**
     * 获取areaCode1属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAreaCode1() {
        return areaCode1;
    }

    /**
     * 设置areaCode1属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAreaCode1(JAXBElement<String> value) {
        this.areaCode1 = value;
    }

    /**
     * 获取attribute1属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute1() {
        return attribute1;
    }

    /**
     * 设置attribute1属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute1(JAXBElement<String> value) {
        this.attribute1 = value;
    }

    /**
     * 获取attribute10属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute10() {
        return attribute10;
    }

    /**
     * 设置attribute10属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute10(JAXBElement<String> value) {
        this.attribute10 = value;
    }

    /**
     * 获取attribute11属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute11() {
        return attribute11;
    }

    /**
     * 设置attribute11属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute11(JAXBElement<String> value) {
        this.attribute11 = value;
    }

    /**
     * 获取attribute12属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute12() {
        return attribute12;
    }

    /**
     * 设置attribute12属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute12(JAXBElement<String> value) {
        this.attribute12 = value;
    }

    /**
     * 获取attribute13属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute13() {
        return attribute13;
    }

    /**
     * 设置attribute13属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute13(JAXBElement<String> value) {
        this.attribute13 = value;
    }

    /**
     * 获取attribute14属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute14() {
        return attribute14;
    }

    /**
     * 设置attribute14属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute14(JAXBElement<String> value) {
        this.attribute14 = value;
    }

    /**
     * 获取attribute15属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute15() {
        return attribute15;
    }

    /**
     * 设置attribute15属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute15(JAXBElement<String> value) {
        this.attribute15 = value;
    }

    /**
     * 获取attribute16属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute16() {
        return attribute16;
    }

    /**
     * 设置attribute16属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute16(JAXBElement<String> value) {
        this.attribute16 = value;
    }

    /**
     * 获取attribute17属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute17() {
        return attribute17;
    }

    /**
     * 设置attribute17属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute17(JAXBElement<String> value) {
        this.attribute17 = value;
    }

    /**
     * 获取attribute18属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute18() {
        return attribute18;
    }

    /**
     * 设置attribute18属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute18(JAXBElement<String> value) {
        this.attribute18 = value;
    }

    /**
     * 获取attribute19属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute19() {
        return attribute19;
    }

    /**
     * 设置attribute19属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute19(JAXBElement<String> value) {
        this.attribute19 = value;
    }

    /**
     * 获取attribute2属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute2() {
        return attribute2;
    }

    /**
     * 设置attribute2属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute2(JAXBElement<String> value) {
        this.attribute2 = value;
    }

    /**
     * 获取attribute20属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute20() {
        return attribute20;
    }

    /**
     * 设置attribute20属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute20(JAXBElement<String> value) {
        this.attribute20 = value;
    }

    /**
     * 获取attribute3属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute3() {
        return attribute3;
    }

    /**
     * 设置attribute3属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute3(JAXBElement<String> value) {
        this.attribute3 = value;
    }

    /**
     * 获取attribute4属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute4() {
        return attribute4;
    }

    /**
     * 设置attribute4属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute4(JAXBElement<String> value) {
        this.attribute4 = value;
    }

    /**
     * 获取attribute5属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute5() {
        return attribute5;
    }

    /**
     * 设置attribute5属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute5(JAXBElement<String> value) {
        this.attribute5 = value;
    }

    /**
     * 获取attribute6属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute6() {
        return attribute6;
    }

    /**
     * 设置attribute6属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute6(JAXBElement<String> value) {
        this.attribute6 = value;
    }

    /**
     * 获取attribute7属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute7() {
        return attribute7;
    }

    /**
     * 设置attribute7属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute7(JAXBElement<String> value) {
        this.attribute7 = value;
    }

    /**
     * 获取attribute8属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute8() {
        return attribute8;
    }

    /**
     * 设置attribute8属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute8(JAXBElement<String> value) {
        this.attribute8 = value;
    }

    /**
     * 获取attribute9属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAttribute9() {
        return attribute9;
    }

    /**
     * 设置attribute9属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAttribute9(JAXBElement<String> value) {
        this.attribute9 = value;
    }

    /**
     * 获取barterFlag属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getBarterFlag() {
        return barterFlag;
    }

    /**
     * 设置barterFlag属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setBarterFlag(JAXBElement<String> value) {
        this.barterFlag = value;
    }

    /**
     * 获取bookingRange属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getBookingRange() {
        return bookingRange;
    }

    /**
     * 设置bookingRange属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setBookingRange(JAXBElement<String> value) {
        this.bookingRange = value;
    }

    /**
     * 获取buyDate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getBuyDate() {
        return buyDate;
    }

    /**
     * 设置buyDate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setBuyDate(JAXBElement<String> value) {
        this.buyDate = value;
    }

    /**
     * 获取createdBy属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置createdBy属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCreatedBy(JAXBElement<String> value) {
        this.createdBy = value;
    }

    /**
     * 获取createdDate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCreatedDate() {
        return createdDate;
    }

    /**
     * 设置createdDate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCreatedDate(JAXBElement<String> value) {
        this.createdDate = value;
    }

    /**
     * 获取customerId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCustomerId() {
        return customerId;
    }

    /**
     * 设置customerId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCustomerId(JAXBElement<String> value) {
        this.customerId = value;
    }

    /**
     * 获取customerName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCustomerName() {
        return customerName;
    }

    /**
     * 设置customerName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCustomerName(JAXBElement<String> value) {
        this.customerName = value;
    }

    /**
     * 获取email属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getEmail() {
        return email;
    }

    /**
     * 设置email属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setEmail(JAXBElement<String> value) {
        this.email = value;
    }

    /**
     * 获取errorInfo属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getErrorInfo() {
        return errorInfo;
    }

    /**
     * 设置errorInfo属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setErrorInfo(JAXBElement<String> value) {
        this.errorInfo = value;
    }

    /**
     * 获取faultDesc属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFaultDesc() {
        return faultDesc;
    }

    /**
     * 设置faultDesc属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFaultDesc(JAXBElement<String> value) {
        this.faultDesc = value;
    }

    /**
     * 获取ifFlush属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getIfFlush() {
        return ifFlush;
    }

    /**
     * 设置ifFlush属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setIfFlush(JAXBElement<String> value) {
        this.ifFlush = value;
    }

    /**
     * 获取ifPullBack属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getIfPullBack() {
        return ifPullBack;
    }

    /**
     * 设置ifPullBack属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setIfPullBack(JAXBElement<String> value) {
        this.ifPullBack = value;
    }

    /**
     * 获取ifService属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getIfService() {
        return ifService;
    }

    /**
     * 设置ifService属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setIfService(JAXBElement<String> value) {
        this.ifService = value;
    }

    /**
     * 获取imageUrl属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置imageUrl属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setImageUrl(JAXBElement<String> value) {
        this.imageUrl = value;
    }

    /**
     * 获取importFlag属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getImportFlag() {
        return importFlag;
    }

    /**
     * 设置importFlag属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setImportFlag(JAXBElement<String> value) {
        this.importFlag = value;
    }

    /**
     * 获取jdWbId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getJdWbId() {
        return jdWbId;
    }

    /**
     * 设置jdWbId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setJdWbId(JAXBElement<String> value) {
        this.jdWbId = value;
    }

    /**
     * 获取lastUpdBy属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLastUpdBy() {
        return lastUpdBy;
    }

    /**
     * 设置lastUpdBy属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLastUpdBy(JAXBElement<String> value) {
        this.lastUpdBy = value;
    }

    /**
     * 获取lastUpdDate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLastUpdDate() {
        return lastUpdDate;
    }

    /**
     * 设置lastUpdDate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLastUpdDate(JAXBElement<String> value) {
        this.lastUpdDate = value;
    }

    /**
     * 获取logoId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLogoId() {
        return logoId;
    }

    /**
     * 设置logoId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLogoId(JAXBElement<String> value) {
        this.logoId = value;
    }

    /**
     * 获取logoName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLogoName() {
        return logoName;
    }

    /**
     * 设置logoName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLogoName(JAXBElement<String> value) {
        this.logoName = value;
    }

    /**
     * 获取lsOrder属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLsOrder() {
        return lsOrder;
    }

    /**
     * 设置lsOrder属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLsOrder(JAXBElement<String> value) {
        this.lsOrder = value;
    }

    /**
     * 获取marketId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMarketId() {
        return marketId;
    }

    /**
     * 设置marketId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMarketId(JAXBElement<String> value) {
        this.marketId = value;
    }

    /**
     * 获取marketName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMarketName() {
        return marketName;
    }

    /**
     * 设置marketName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMarketName(JAXBElement<String> value) {
        this.marketName = value;
    }

    /**
     * 获取midProductId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMidProductId() {
        return midProductId;
    }

    /**
     * 设置midProductId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMidProductId(JAXBElement<String> value) {
        this.midProductId = value;
    }

    /**
     * 获取midProductName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMidProductName() {
        return midProductName;
    }

    /**
     * 设置midProductName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMidProductName(JAXBElement<String> value) {
        this.midProductName = value;
    }

    /**
     * 获取modelCode属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModelCode() {
        return modelCode;
    }

    /**
     * 设置modelCode属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModelCode(JAXBElement<String> value) {
        this.modelCode = value;
    }

    /**
     * 获取modelId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModelId() {
        return modelId;
    }

    /**
     * 设置modelId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModelId(JAXBElement<String> value) {
        this.modelId = value;
    }

    /**
     * 获取modelName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModelName() {
        return modelName;
    }

    /**
     * 设置modelName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModelName(JAXBElement<String> value) {
        this.modelName = value;
    }

    /**
     * 获取modificationNum属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModificationNum() {
        return modificationNum;
    }

    /**
     * 设置modificationNum属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModificationNum(JAXBElement<String> value) {
        this.modificationNum = value;
    }

    /**
     * 获取openFlag属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getOpenFlag() {
        return openFlag;
    }

    /**
     * 设置openFlag属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setOpenFlag(JAXBElement<String> value) {
        this.openFlag = value;
    }

    /**
     * 获取originFlag属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getOriginFlag() {
        return originFlag;
    }

    /**
     * 设置originFlag属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setOriginFlag(JAXBElement<String> value) {
        this.originFlag = value;
    }

    /**
     * 获取price属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getPrice() {
        return price;
    }

    /**
     * 设置price属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setPrice(JAXBElement<String> value) {
        this.price = value;
    }

    /**
     * 获取prodSerialNo属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getProdSerialNo() {
        return prodSerialNo;
    }

    /**
     * 设置prodSerialNo属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setProdSerialNo(JAXBElement<String> value) {
        this.prodSerialNo = value;
    }

    /**
     * 获取prodTypeId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getProdTypeId() {
        return prodTypeId;
    }

    /**
     * 设置prodTypeId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setProdTypeId(JAXBElement<String> value) {
        this.prodTypeId = value;
    }

    /**
     * 获取projectNo属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getProjectNo() {
        return projectNo;
    }

    /**
     * 设置projectNo属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setProjectNo(JAXBElement<String> value) {
        this.projectNo = value;
    }

    /**
     * 获取psnAccount属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getPsnAccount() {
        return psnAccount;
    }

    /**
     * 设置psnAccount属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setPsnAccount(JAXBElement<String> value) {
        this.psnAccount = value;
    }

    /**
     * 获取psnPwd属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getPsnPwd() {
        return psnPwd;
    }

    /**
     * 设置psnPwd属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setPsnPwd(JAXBElement<String> value) {
        this.psnPwd = value;
    }

    /**
     * 获取regoinId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRegoinId() {
        return regoinId;
    }

    /**
     * 设置regoinId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRegoinId(JAXBElement<String> value) {
        this.regoinId = value;
    }

    /**
     * 获取remark属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRemark() {
        return remark;
    }

    /**
     * 设置remark属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRemark(JAXBElement<String> value) {
        this.remark = value;
    }

    /**
     * 获取requireDesc属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRequireDesc() {
        return requireDesc;
    }

    /**
     * 设置requireDesc属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRequireDesc(JAXBElement<String> value) {
        this.requireDesc = value;
    }

    /**
     * 获取requireServiceDate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRequireServiceDate() {
        return requireServiceDate;
    }

    /**
     * 设置requireServiceDate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRequireServiceDate(JAXBElement<String> value) {
        this.requireServiceDate = value;
    }

    /**
     * 获取roadId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRoadId() {
        return roadId;
    }

    /**
     * 设置roadId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRoadId(JAXBElement<String> value) {
        this.roadId = value;
    }

    /**
     * 获取rowId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getRowId() {
        return rowId;
    }

    /**
     * 设置rowId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setRowId(JAXBElement<String> value) {
        this.rowId = value;
    }

    /**
     * 获取saleOrderId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSaleOrderId() {
        return saleOrderId;
    }

    /**
     * 设置saleOrderId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSaleOrderId(JAXBElement<String> value) {
        this.saleOrderId = value;
    }

    /**
     * 获取saleOrderNo属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSaleOrderNo() {
        return saleOrderNo;
    }

    /**
     * 设置saleOrderNo属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSaleOrderNo(JAXBElement<String> value) {
        this.saleOrderNo = value;
    }

    /**
     * 获取saleOrderNoSon属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSaleOrderNoSon() {
        return saleOrderNoSon;
    }

    /**
     * 设置saleOrderNoSon属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSaleOrderNoSon(JAXBElement<String> value) {
        this.saleOrderNoSon = value;
    }

    /**
     * 获取serverId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getServerId() {
        return serverId;
    }

    /**
     * 设置serverId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setServerId(JAXBElement<String> value) {
        this.serverId = value;
    }

    /**
     * 获取serviceModeId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getServiceModeId() {
        return serviceModeId;
    }

    /**
     * 设置serviceModeId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setServiceModeId(JAXBElement<String> value) {
        this.serviceModeId = value;
    }

    /**
     * 获取serviceTypeId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getServiceTypeId() {
        return serviceTypeId;
    }

    /**
     * 设置serviceTypeId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setServiceTypeId(JAXBElement<String> value) {
        this.serviceTypeId = value;
    }

    /**
     * 获取telephone1属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTelephone1() {
        return telephone1;
    }

    /**
     * 设置telephone1属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTelephone1(JAXBElement<String> value) {
        this.telephone1 = value;
    }

    /**
     * 获取telephone2属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTelephone2() {
        return telephone2;
    }

    /**
     * 设置telephone2属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTelephone2(JAXBElement<String> value) {
        this.telephone2 = value;
    }

    /**
     * 获取telephone3属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTelephone3() {
        return telephone3;
    }

    /**
     * 设置telephone3属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTelephone3(JAXBElement<String> value) {
        this.telephone3 = value;
    }

    /**
     * 获取totalNum属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTotalNum() {
        return totalNum;
    }

    /**
     * 设置totalNum属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTotalNum(Integer value) {
        this.totalNum = value;
    }

    /**
     * 获取userIp属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUserIp() {
        return userIp;
    }

    /**
     * 设置userIp属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUserIp(JAXBElement<String> value) {
        this.userIp = value;
    }

    /**
     * 获取userIpDate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUserIpDate() {
        return userIpDate;
    }

    /**
     * 设置userIpDate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUserIpDate(JAXBElement<String> value) {
        this.userIpDate = value;
    }

    /**
     * 获取videoUrl属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getVideoUrl() {
        return videoUrl;
    }

    /**
     * 设置videoUrl属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setVideoUrl(JAXBElement<String> value) {
        this.videoUrl = value;
    }

    /**
     * 获取weChatNc属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getWeChatNc() {
        return weChatNc;
    }

    /**
     * 设置weChatNc属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setWeChatNc(JAXBElement<String> value) {
        this.weChatNc = value;
    }

    /**
     * 获取wxOpenid属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getWxOpenid() {
        return wxOpenid;
    }

    /**
     * 设置wxOpenid属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setWxOpenid(JAXBElement<String> value) {
        this.wxOpenid = value;
    }

    /**
     * 获取wzCustomerId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getWzCustomerId() {
        return wzCustomerId;
    }

    /**
     * 设置wzCustomerId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setWzCustomerId(JAXBElement<String> value) {
        this.wzCustomerId = value;
    }

    /**
     * 获取wzRowId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getWzRowId() {
        return wzRowId;
    }

    /**
     * 设置wzRowId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setWzRowId(JAXBElement<String> value) {
        this.wzRowId = value;
    }

    /**
     * 获取zxContent属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getZxContent() {
        return zxContent;
    }

    /**
     * 设置zxContent属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setZxContent(JAXBElement<String> value) {
        this.zxContent = value;
    }

    /**
     * 获取zxMode属性的值。
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getZxMode() {
        return zxMode;
    }

    /**
     * 设置zxMode属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setZxMode(JAXBElement<String> value) {
        this.zxMode = value;
    }

}
