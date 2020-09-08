package com.szeastroc.icebox.util.wechatpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "weixin.store")
public class WeiXinConfig {

	public static final String WEI_XIN_URL = "https://api.weixin.qq.com";

	public static final String GET_XIAO_USER_INFO = WEI_XIN_URL + "/sns/jscode2session";

	private String appId;

	private String appSecret;

	private String mchId;

	private String secret;

	private String notifyUrl;

	private String certUrl;

	private Order order;

	private String dmsappId;

	private String dmsappSecret;

	@Data
	public static class Order{

		private long timeout;
	}


}
