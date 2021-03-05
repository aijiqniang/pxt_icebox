
package com.szeastroc.icebox.newprocess.webservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 *
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="request" type="{http://vo.website.webservice.net.crm.neusoft.com/xsd}WbSiteRequestVO" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "request"
})
@XmlRootElement(name = "getWBMail")
public class GetWBMail {

    @XmlElementRef(name = "request", namespace = "http://action.website.webservice.net.crm.neusoft.com", type = JAXBElement.class, required = false)
    protected JAXBElement<WbSiteRequestVO> request;

    /**
     * ��ȡrequest���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link WbSiteRequestVO }{@code >}
     *     
     */
    public JAXBElement<WbSiteRequestVO> getRequest() {
        return request;
    }

    /**
     * ����request���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link WbSiteRequestVO }{@code >}
     *     
     */
    public void setRequest(JAXBElement<WbSiteRequestVO> value) {
        this.request = value;
    }

}
