package com.szeastroc.icebox.util;


import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class PxtSecurityUtils {


    private static final String KEY = "dp2018%*_+#@*(*&dongpeng";

    /**
     * 加密
     * @param text
     * @return
     */
    public static String encrypt(String text){
        try {
            return encryptThreeDESECB(text,KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密
     * @param text
     * @return
     */
    public static String decrypt(String text){
        try {
            return decryptThreeDESECB(text,KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3DESECB加密
     * @param src 要进行了加密的原文
     * @param key 密钥   key必须是长度大于等于 3*8 = 24 位
     * @return
     * @throws Exception
     */
    public static String encryptThreeDESECB(String src,String key) throws Exception{
        DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
        SecretKey securekey = keyFactory.generateSecret(dks);

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, securekey);
        byte[] b=cipher.doFinal(src.getBytes());

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(b).replaceAll("\r", "").replaceAll("\n", "");

    }

    /**
     * 3DESECB解密
     * @param src 要解密的密文字符
     * @param key 解密的Key key必须是长度大于等于 3*8 = 24 位
     * @return
     * @throws Exception
     */
    public static String decryptThreeDESECB(String src,String key) throws Exception {
        //--通过base64,将字符串转成byte数组
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] bytesrc = decoder.decodeBuffer(src);
        //--解密的key
        DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
        SecretKey securekey = keyFactory.generateSecret(dks);

        //--Chipher对象解密
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, securekey);
        byte[] retByte = cipher.doFinal(bytesrc);

        return new String(retByte);
    }
}
