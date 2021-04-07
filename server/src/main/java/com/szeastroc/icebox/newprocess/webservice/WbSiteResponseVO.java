
package com.szeastroc.icebox.newprocess.webservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>WbSiteResponseVO complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType name="WbSiteResponseVO">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="result_code" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="result_msg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WbSiteResponseVO", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", propOrder = {
    "resultCode",
    "resultMsg"
})
public class WbSiteResponseVO {

    @XmlElementRef(name = "result_code", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> resultCode;
    @XmlElementRef(name = "result_msg", namespace = "http://vo.website.webservice.net.crm.neusoft.com/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> resultMsg;

    /**
     * ��ȡresultCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getResultCode() {
        return resultCode;
    }

    /**
     * ����resultCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setResultCode(JAXBElement<String> value) {
        this.resultCode = value;
    }

    /**
     * ��ȡresultMsg���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getResultMsg() {
        return resultMsg;
    }

    /**
     * ����resultMsg���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setResultMsg(JAXBElement<String> value) {
        this.resultMsg = value;
    }

}
