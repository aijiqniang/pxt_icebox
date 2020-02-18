package com.szeastroc.icebox.util;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by Tulane
 * 2019/5/23
 */
@Slf4j
public class CommonUtil {

    public static String generateOrderNumber(){
        DateTime dt = new DateTime();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String number = dt.toString("yyyyMMddHHmmss") + timeStamp.substring(timeStamp.length() - 4, timeStamp.length()) + uuid.substring(0, 14);
        return number;
    }

    public static void assertNullObj(Object obj) throws ImproperOptionException {
        if(obj == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
    }

    /**
     * 解析xml至对象
     *
     * @param request
     * @return
     * @throws Exception
     */
    public static OrderPayBack xmlToObj(HttpServletRequest request) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));

        StringBuilder wholeStr = new StringBuilder();
        String str = "";
        while ((str = reader.readLine()) != null) {//一行一行的读取body体里面的内容；
            wholeStr.append(str);
        }

        // post请求的密文数据
        // sReqData = HttpUtils.PostData();
        String sReqData = wholeStr.toString();
        log.info("sReqData: {}", sReqData);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        StringReader sr = new StringReader(sReqData);
        InputSource is = new InputSource(sr);
        Document document = db.parse(is);

        Element root = document.getDocumentElement();

        OrderPayBack orderPayBack = new OrderPayBack();

        NodeList nodelist1 = root.getElementsByTagName("return_code");
        String returnCode = nodelist1.item(0).getTextContent();

        orderPayBack.setReturnCode(returnCode);

        NodeList nodelist8 = root.getElementsByTagName("return_msg");
        if (nodelist8 != null && nodelist8.item(0) != null) {
            String returnMsg = nodelist8.item(0).getTextContent();
            orderPayBack.setReturnMsg(returnMsg);
        }

        if (returnCode.equals("SUCCESS")) {

            NodeList nodelist2 = root.getElementsByTagName("openid");
            NodeList nodelist3 = root.getElementsByTagName("total_fee");
            NodeList nodelist4 = root.getElementsByTagName("out_trade_no");
            NodeList nodelist5 = root.getElementsByTagName("transaction_id");
            NodeList nodelist6 = root.getElementsByTagName("time_end");
            NodeList nodelist7 = root.getElementsByTagName("result_code");

            String openid = nodelist2.item(0).getTextContent();
            String totalFee = nodelist3.item(0).getTextContent();
            String outTradeNo = nodelist4.item(0).getTextContent();
            String transactionId = nodelist5.item(0).getTextContent();
            String timeEnd = nodelist6.item(0).getTextContent();
            String resultCode = nodelist7.item(0).getTextContent();

            orderPayBack.setOpenid(openid);
            orderPayBack.setTotalFee(totalFee);
            orderPayBack.setOutTradeNo(outTradeNo);
            orderPayBack.setTransactionId(transactionId);
            orderPayBack.setTimeEnd(timeEnd);
            orderPayBack.setResultCode(resultCode);
        }

        log.info("回调数据 -> {}", JSON.toJSONString(orderPayBack));
        return orderPayBack;
    }
}
