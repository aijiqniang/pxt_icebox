
package com.szeastroc.icebox.newprocess.webservice;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "WebSite", targetNamespace = "http://action.website.webservice.net.crm.neusoft.com", wsdlLocation = "http://172.16.41.13/HisenseCRMWS/services/WebSite?wsdl")
public class WebSite
    extends Service
{

    private final static URL WEBSITE_WSDL_LOCATION;
    private final static WebServiceException WEBSITE_EXCEPTION;
    private final static QName WEBSITE_QNAME = new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSite");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://172.16.41.13/HisenseCRMWS/services/WebSite?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WEBSITE_WSDL_LOCATION = url;
        WEBSITE_EXCEPTION = e;
    }

    public WebSite() {
        super(__getWsdlLocation(), WEBSITE_QNAME);
    }

    public WebSite(WebServiceFeature... features) {
        super(__getWsdlLocation(), WEBSITE_QNAME, features);
    }

    public WebSite(URL wsdlLocation) {
        super(wsdlLocation, WEBSITE_QNAME);
    }

    public WebSite(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, WEBSITE_QNAME, features);
    }

    public WebSite(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WebSite(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpSoap11Endpoint")
    public WebSitePortType getWebSiteHttpSoap11Endpoint() {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpSoap11Endpoint"), WebSitePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpSoap11Endpoint")
    public WebSitePortType getWebSiteHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpSoap11Endpoint"), WebSitePortType.class, features);
    }

    /**
     * 
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpSoap12Endpoint")
    public WebSitePortType getWebSiteHttpSoap12Endpoint() {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpSoap12Endpoint"), WebSitePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpSoap12Endpoint")
    public WebSitePortType getWebSiteHttpSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpSoap12Endpoint"), WebSitePortType.class, features);
    }

    /**
     * 
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpEndpoint")
    public WebSitePortType getWebSiteHttpEndpoint() {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpEndpoint"), WebSitePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WebSitePortType
     */
    @WebEndpoint(name = "WebSiteHttpEndpoint")
    public WebSitePortType getWebSiteHttpEndpoint(WebServiceFeature... features) {
        return super.getPort(new QName("http://action.website.webservice.net.crm.neusoft.com", "WebSiteHttpEndpoint"), WebSitePortType.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WEBSITE_EXCEPTION!= null) {
            throw WEBSITE_EXCEPTION;
        }
        return WEBSITE_WSDL_LOCATION;
    }

}
