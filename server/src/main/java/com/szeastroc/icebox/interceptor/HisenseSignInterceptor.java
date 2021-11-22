package com.szeastroc.icebox.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.szeastroc.common.bean.ErrorCode;
import com.szeastroc.common.bean.Result;
import com.szeastroc.icebox.util.MD5;
import com.szeastroc.icebox.util.RequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * 海信外部接口签名拦截器
 * @author yuqi9
 * @since 2019/5/23
 */
@Slf4j
@Component
public class HisenseSignInterceptor implements HandlerInterceptor {

    @Value("${hisense.secretKey}")
    private String secretKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        //获取参数
        RequestWrapper requestWrapper = new RequestWrapper(request);
        String jsonBody = requestWrapper.getBody();
//        BufferedReader reader = new BufferedReader( new InputStreamReader(request.getInputStream(), "UTF-8"));
//        String line;
//        String jsonBody = "";
//        while ((line = reader.readLine()) != null){
//            jsonBody = jsonBody.concat(line);
//        }
        //log.info("HisenseSignInterceptor:jsonBody:"+jsonBody);
        // 接收参数
        HashMap map = null;
        try {
            map = JSONObject.parseObject(jsonBody, HashMap.class);
        }catch (JSONException e){
            Result result = new Result();
            result.setCode(ErrorCode.FAILED.getCode());
            result.setMsg("JSON格式错误");
            response.setStatus(Integer.parseInt(ErrorCode.FAILED.getCode()));
            renderJson(response, JSON.toJSONString(result));
        }
        List<String> list = new ArrayList<String>();
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if(null != value && !key.equals("sign")){
                if(value instanceof String ){
                    if(!StringUtils.isBlank(String.valueOf(value))){
                        list.add(String.format("%s=%s", key, value));
                    }
                }else{
                    list.add(String.format("%s=%s", key, value));
                }
            }
        }
        //LIST排序
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        //拼接参数
        for (String s : list) {
            builder.append(s).append("&");
        }
        builder.append("key=");
        builder.append(secretKey);
        //log.info("签名参数:"+ builder.toString());
        //MD5加密 并转大写
        String signCheck = MD5.md5(builder.toString()).toUpperCase();
        //log.info("签名结果:"+signCheck + ",提交签名:"+map.get("sign"));
        //对比签名
        if(!signCheck.equals(map.get("sign"))){
            Result result = new Result();
            result.setCode(ErrorCode.FAILED.getCode());
            result.setMsg("签名错误");
            response.setStatus(Integer.parseInt(ErrorCode.FAILED.getCode()));
            renderJson(response, JSON.toJSONString(result));
            return false;
        }
        return true;
    }

    private void renderJson(HttpServletResponse response, String jsonText) {
        PrintWriter writer = null;
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            writer = response.getWriter();
            writer.write(jsonText);
            writer.close();
        } catch (IOException e) {

        }
    }


}
