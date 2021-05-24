package com.szeastroc.icebox.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/17 16:31
 */
@Slf4j
public class SendRequestUtils {

    private  static ObjectMapper objectMapper = new ObjectMapper();

    public static String sendPostRequest(String url, Map params){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        // 以json的方式提交
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", (String) params.get("type"));
        node.put("relateCode",(String) params.get("relateCode"));
        node.put("pxtNumber", (String)params.get("pxtNumber"));
        if(params.get("id") != null){
            node.put("id", Integer.parseInt((String) params.get("id")));
        }
        //将请求头部和参数合成一个请求
        HttpEntity<String> request = new HttpEntity<String>(node.toString(), headers);

        String result = restTemplate.postForObject(url, request, String.class);
        log.info("发送dms请求返回值-->[{}]",result);
        return result;
    }
}
