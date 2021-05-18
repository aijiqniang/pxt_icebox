package com.szeastroc.icebox.util;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/17 16:31
 */
public class SendRequestUtils {
    public static ResponseEntity<String> sendPostRequest(String url, MultiValueMap<String, String> params){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity( url, requestEntity , String.class );
        System.out.println(response.getBody());
        return response;
    }
}
