package com.szeastroc.icebox.util;

import java.security.MessageDigest;

/**
 * 通用工具类 
 * author:panjianping 
 */
public class MD5 {
	/** MD5加密 */
	public static String md5(String value) {
		StringBuilder result = new StringBuilder();
		try {
			// 实例化MD5加载类
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			// 得到字节数据
			byte[] data = md5.digest(value.getBytes("UTF-8"));
			result.append(byte2hex(data));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 返回结果
		return result.toString();
	}

	public static String byte2hex(byte[] data) {
		StringBuilder result = new StringBuilder();
		for (byte b : data) {
			// 将二进制转换成字符串
			String temp = Integer.toHexString(b & 0XFF);
			// 追加加密后的内容
			if (temp.length() == 1) { // 判断字符长度
				result.append("0").append(temp);
			} else {
				result.append(temp);
			}
		}
		return result.toString();
	}
}
