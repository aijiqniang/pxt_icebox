package com.szeastroc.icebox.util.wechatpay;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.HttpUtils;
import com.szeastroc.icebox.newprocess.enums.OrderSourceEnums;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WeiXinService {

	@Resource
	private WeiXinConfig weiXinConfig;

	public String getOpenid(String code) {
		StringBuilder requestUrl = new StringBuilder(WeiXinConfig.GET_XIAO_USER_INFO);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("appid", weiXinConfig.getAppId());
		params.put("secret", weiXinConfig.getAppSecret());
		params.put("js_code", code);
		params.put("grant_type", "authorization_code");
		String result = HttpUtils.get(requestUrl.toString(), params);
		System.out.println(result);
		return JSON.parseObject(result).getString("openid");
	}

	public String createWeiXinPay(ClientInfoRequest clientInfoRequest, BigDecimal money, String orderNumber, String openid) {
		try {
			Map<String, String> data = new HashMap<String, String>();
			if(OrderSourceEnums.OTOC.getType().equals(clientInfoRequest.getOrderSource())){
				data.put("appid", weiXinConfig.getAppId());
			}else {
				data.put("appid", weiXinConfig.getDmsappId());
			}

			data.put("mch_id", weiXinConfig.getMchId());
			data.put("nonce_str", WXPayUtil.generateNonceStr());
			data.put("body", "E人E店");
			data.put("out_trade_no", orderNumber);
			data.put("total_fee", String.valueOf(money.multiply(new BigDecimal(100)).intValue()));
			data.put("spbill_create_ip", clientInfoRequest.getIp());
			data.put("notify_url", weiXinConfig.getNotifyUrl());
			data.put("trade_type", "JSAPI");
			data.put("openid", openid);
			String xml = WXPayUtil.generateSignedXml(data, weiXinConfig.getSecret());
			String result = requestOnce("https://api.mch.weixin.qq.com/pay/unifiedorder", xml);
			log.info("微信统一下单接口 -> {}", result);
			Map<String, String> payMap = WXPayUtil.xmlToMap(result);
			if ("SUCCESS".equals(payMap.get("return_code"))) {
				return payMap.get("prepay_id");
			} else {
				log.info("错误:微信统一下单接口 -> {}, {}, {}, {}", result, money, orderNumber, openid);
				throw new ImproperOptionException("创建微信订单失败");
			}
		} catch (Exception e) {
			return null;
		}
	}

	public String closeWeiXinPay(String orderNumber){
		try {
			Map<String, String> data = new HashMap<String, String>();
			data.put("appid", weiXinConfig.getAppId());
			data.put("mch_id", weiXinConfig.getMchId());
			data.put("out_trade_no", orderNumber);
			data.put("nonce_str", WXPayUtil.generateNonceStr());
			String xml = WXPayUtil.generateSignedXml(data, weiXinConfig.getSecret());
			String result = requestOnce("https://api.mch.weixin.qq.com/pay/closeorder", xml);
			log.info("微信关闭订单接口 -> {}", result);
			Map<String, String> payMap = WXPayUtil.xmlToMap(result);
			if ("SUCCESS".equals(payMap.get("return_code"))) {
				return payMap.get("prepay_id");
			} else {
				log.info("错误:微信关闭订单接口 -> {}, {}", result, orderNumber);
				throw new ImproperOptionException("微信关闭订单失败");
			}
		} catch (Exception e) {
			return null;
		}
	}

	public String requestOnce(final String url, String data) throws Exception {
		BasicHttpClientConnectionManager connManager;

		connManager = new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory()).build(), null, null, null);

		HttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connManager).build();

		HttpPost httpPost = new HttpPost(url);

		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build();
		httpPost.setConfig(requestConfig);

		StringEntity postEntity = new StringEntity(data, "UTF-8");
		httpPost.addHeader("Content-Type", "text/xml");
		httpPost.setEntity(postEntity);

		HttpResponse httpResponse = httpClient.execute(httpPost);
		HttpEntity httpEntity = httpResponse.getEntity();
		return EntityUtils.toString(httpEntity, "UTF-8");

	}
}
